const debug = require('debug')('external:payment-service');
const requestUserPayment = (user, paymentDetails) => {
    if (!paymentDetails) {
        return Promise.reject( 'no payment details supplied');
    }
    if (paymentDetails && paymentDetails.amount <= 0) {
        return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
        debug(`Getting ${paymentDetails.amount / 100}€ from ${user.last_name}, ${user.first_name}...`);
        setTimeout(() => {
            if (getRandomBoolean()) {
                resolve(paymentDetails.amount)
            } else {
                reject(-paymentDetails.amount);
            }
        }, 1000);
    })
};

const getRandomBoolean = () => {
    const tP = 60;
    return tP >= getRandomInt(0, 100)
};

const getRandomInt = (min, max) => {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min)) + min;
};

module.exports = requestUserPayment;
