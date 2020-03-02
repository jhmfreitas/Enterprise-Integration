const requestUserPayment = require('../util/payment-service-api');
const debug = require('debug')('kafka:maas');
const models = require("../models/user");
const resolverFactory = (app) => {
    const userModel = models(app.locals.db);

    const paymentOrderResolver = (message) => {
        const userToken = message.token;
        const paymentDetails = message.payment_details;
        userModel.getUser(userToken)
            .then(user => requestUserPayment(user, paymentDetails))
            .then()
            .catch((amount) => {
                userModel.updateBalance(message.token, amount)
                    .then(isBlacklisted => {
                        if (isBlacklisted) {
                            return Promise.all(app.locals.broker.producer.sendAll(JSON.stringify({
                                type: 'blacklist',
                                user: userToken
                            })))
                        }
                        return [];
                    })
                    .then(responses => {
                        responses.map(response => {
                            debug(response)
                        })
                    })

            })

    };

    const newOperatorResolver = (message) => {
        const newTopic = message.operator;
        if (!app.locals.transportationTopics.includes(newTopic)) {
            app.locals.transportationTopics.push(newTopic);
            app.locals.broker.consumer.addTopics(newTopic, ((error, added) => {
                if (error) {
                    debug(`Error adding topic: ${error}`);
                } else {
                    debug('Topic ' + newTopic)
                }
            }))
        }
    };

    const maasResolvers = {
        'payment-order': paymentOrderResolver,
        'new-operator': newOperatorResolver,
    };

    return (event) => {
        const message = event.value;
        if (message.type in maasResolvers) {
            maasResolvers[message.type](message);
        } else {
            debug(`Event ${message.type} resolver not implemented`)
        }
    };
};

module.exports = resolverFactory;
