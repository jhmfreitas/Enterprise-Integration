const express = require('express');
const requestUserPayment = require('../../util/payment-service-api');
const debug = require('debug')('routes:users');
const router = (db) => {
    const eRouter = new express.Router();
    const userModel = require("../../models/user")(db);
    eRouter.get('/', function(req, res, next) {
       userModel.getUsers()
           .then(users => {
               res.status(200).json(users)
           })
           .catch(err => {
               debug(err);
               res.sendStatus(400)
           })
    });
    /* Add new user listing. */
    eRouter.post('/', function (req, res, next) {
        const user = req.body.user;
        console.log(req.body)
        userModel.addUser(user)
            .then(id => {
                user.id = id;
                return user;
            })
            .then(user => {
                res.status(200).json(user);
                return user;
            })
            .catch(err => {
                res.sendStatus(400);
                Promise.reject(err);
            })
            .then(user => {
                return req.app.locals.broker.producer.sendp([
                    {
                        topic: 'maas', messages: JSON.stringify({
                            type: 'new-user',
                            user
                        })
                    }
                ])
            })
            .then(data => {
                // Handle?
            })
            .catch(err => {
                console.log('new-user event error');
                console.warn(err.message)
            })
    });

    eRouter.get('/(:userId)', (req, res, next) => {
        userModel.getUser(req.params.userId)
            .then(user => {
                res.status(200).json(user);
            })
            .catch(err => {
                console.log(err);
                res.sendStatus(404);
            })
    });

    eRouter.get('/(:userId)/attempt-payment', (req, res, next) => {
        userModel.getUser(req.params.userId)
            .then(user => {
                if (user.is_blacklisted) {
                    return requestUserPayment(user, {amount: -user.balance})
                } else {
                    return Promise.reject();
                }
            })
            .then((amount) =>
                userModel.updateBalance(req.params.userId, amount))
            .then(isBlacklisted => {
                if (isBlacklisted) {
                    debug('User is still blacklisted');
                    res.status(400).json({
                        errors: [
                            {
                                message: 'User has no founds'
                            }
                        ]
                    })
                } else {
                    Promise.all(req.app.locals.broker.producer.sendAll(JSON.stringify({
                        type: 'whitelist',
                        user: req.params.userId
                    }))).then(responses => {
                        responses.map(response => {
                            debug(response)
                        })
                    });
                    res.status(200)
                        .json({
                            message:
                                'User payment successful'
                        })

                }
                debug(`\nUser ${req.params.userId} is ${(isBlacklisted ? "" : "not ")} blacklisted`);
                return Promise.resolve();
            })
            .catch((err) => {
                debug((err ? err : 'User debt is 0'));
                res.status(400).json({
                    errors: [
                        {
                            message: 'User debt is 0'
                        }
                    ]
                });
            })
    });
    return eRouter;
};

module.exports = router;
