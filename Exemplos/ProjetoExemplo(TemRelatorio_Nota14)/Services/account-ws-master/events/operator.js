const debug = require('debug')('kafka:operator');
const catalog = require('../util/catalog-service-api');
const DISCOUNT_MIN_TRIPS = 2;
const DISCONT_MULT = 0.9;
const DISCOUNT_ADD = 0;
const resolverFactory = (app) => {
    const userModel = require("../models/user")(app.locals.db);
    return (event) => {
        debug('received event');
        const message = event.value;
        const operator = event.topic;
        const serviceName = message.service_name;
        switch (message.type) {
            case 'flat-fare':
            case 'check-out':
            case 'operator-defined':
                catalog.validateService(operator, serviceName, message.type)
                    .then(() => {
                        return userModel.addTrip(message.token)
                    })
                    .then((trips) => {
                        if (trips === DISCOUNT_MIN_TRIPS) {
                            return app.locals.broker.producer.sendp(
                                [
                                    {
                                        topic: 'maas', messages: JSON.stringify({
                                            type: 'discount',
                                            timestamp: message.timestamp,
                                            token: message.token,
                                            value: {
                                                a: DISCONT_MULT,
                                                b: DISCOUNT_ADD
                                            }
                                        })
                                    }
                                ]
                            ).then(() => {
                                debug(`Sent discount event for user: ${message.token}`)
                            })
                        } else {

                        }
                    })
                    .catch((err) => {
                        debug(err);
                    });
                break;
            default:
                break;
        }
    }
};

module.exports = resolverFactory;


