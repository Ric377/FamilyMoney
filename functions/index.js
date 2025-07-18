// D:/Android/Projects/FamilyMoney/functions/index.js

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Эта функция будет срабатывать каждый раз, когда
 * в базу данных добавляется новая трата.
 */
exports.sendPaymentNotification = functions.firestore
    .document("groups/{groupId}/payments/{paymentId}")
    .onCreate(async (snap, context) => {
        // 1. Получаем данные о новой трате
        const paymentData = snap.data();
        const creatorName = paymentData.name;
        const sum = paymentData.sum;
        const groupId = context.params.groupId;
        const creatorEmail = paymentData.email; // Берём email из документа траты

        if (!creatorEmail) {
            console.log("Creator email not found in payment document.");
            return null;
        }

        // 2. Получаем список всех участников группы
        const groupDoc = await admin.firestore().collection("groups").document(groupId).get();
        if (!groupDoc.exists) {
            console.log("Group not found:", groupId);
            return null;
        }
        const members = groupDoc.data().members;

        // 3. Находим email всех, кому нужно отправить уведомление (все, кроме создателя)
        const recipientEmails = members
            .map((member) => member.email)
            .filter((email) => email !== creatorEmail);

        if (recipientEmails.length === 0) {
            console.log("No other members in the group to notify.");
            return null;
        }

        // 4. Находим их телефоны (FCM токены) в базе
        const usersRef = admin.firestore().collection("users");
        const recipientUsersSnapshot = await usersRef.where("email", "in", recipientEmails).get();

        const tokens = recipientUsersSnapshot.docs
            .map((doc) => doc.data().fcmToken)
            .filter((token) => token); // Убираем пустые/отсутствующие токены

        if (tokens.length === 0) {
            console.log("No FCM tokens found for any recipients.");
            return null;
        }

        // 5. Создаём и отправляем уведомление
        const payload = {
            notification: {
                title: `Новая трата в группе!`,
                body: `${creatorName} добавил(а) трату на ${sum} ₽`,
            },
        };

        console.log("Sending notification to tokens:", tokens);
        return admin.messaging().sendToDevice(tokens, payload);
    });