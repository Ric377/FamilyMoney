// D:/Android/Projects/FamilyMoney/functions/index.js

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({region: "europe-west1"});

/**
 * Триггер, который срабатывает при создании нового документа
 * в любой подколлекции 'payments'.
 */
exports.sendPaymentNotification = onDocumentCreated("groups/{groupId}/payments/{paymentId}", async (event) => {
    // 1. Получаем данные о новой трате
    const snap = event.data;
    if (!snap) {
        logger.log("No data associated with the event");
        return;
    }
    const paymentData = snap.data();
    const creatorName = paymentData.name;
    const sum = paymentData.sum;
    const groupId = event.params.groupId;
    const creatorEmail = paymentData.email;

    if (!creatorEmail) {
        logger.log("Creator email not found in payment document.");
        return;
    }

    // 2. Получаем список всех участников группы
    // ИСПРАВЛЕНИЕ ЗДЕСЬ: .document() заменено на .doc()
    const groupDoc = await admin.firestore().collection("groups").doc(groupId).get();
    if (!groupDoc.exists) {
        logger.log("Group not found:", groupId);
        return;
    }
    const members = groupDoc.data().members;

    // 3. Находим email всех, кому нужно отправить уведомление (все, кроме создателя)
    const recipientEmails = members
        .map((member) => member.email)
        .filter((email) => email !== creatorEmail);

    if (recipientEmails.length === 0) {
        logger.log("No other members in the group to notify.");
        return;
    }

    // 4. Находим их FCM токены в базе
    const usersRef = admin.firestore().collection("users");
    const recipientUsersSnapshot = await usersRef.where("email", "in", recipientEmails).get();

    const tokens = recipientUsersSnapshot.docs
        .map((doc) => doc.data().fcmToken)
        .filter((token) => token); // Отсеиваем пустые или отсутствующие токены

    if (tokens.length === 0) {
        logger.log("No FCM tokens found for any recipients.");
        return;
    }

    // 5. Создаём и отправляем уведомление
    const payload = {
        data: {
            title: `Новая трата в группе!`,
            body: `${creatorName} добавил(а) трату на ${sum} ₽`,
        },
    };

    logger.log("Sending data message to tokens:", tokens);

    // Отправляем сообщение на все найденные токены
    const response = await admin.messaging().sendToDevice(tokens, payload);

    // (Опционально) Добавляем обработку ошибок
    response.results.forEach((result, index) => {
        const error = result.error;
        if (error) {
            logger.error("Failure sending notification to", tokens[index], error);
        }
    });

    return response;
});