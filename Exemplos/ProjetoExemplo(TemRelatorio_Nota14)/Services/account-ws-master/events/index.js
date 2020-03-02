const kafka = require('kafka-node');
const maasHandler = require('./maas');
const fetch = require('node-fetch');
const operatorHandler = require('./operator')
const debugClient = require('debug')('kafka:client');
const debugProducer = require('debug')('kafka:producer');
const debugConsumer = require('debug')('kafka:consumer');
const resolvers = {};
const setup = (app) => {

    /* Print latest offset. */
    return getOperators().then(topics => {
        const client = new kafka.KafkaClient({kafkaHost: process.env.KAFKA_BROKERS_HOST});
        const producer = new kafka.Producer(client);
        debugClient('Connecting to kafka...');
        const consumer = new kafka.Consumer(client, [
            {
                topic: 'maas',
            },
        ].concat(topics), {
            fromOffset: false,
        });

        consumer.on('message', (event) => {
            try {
                if (event) {
                    event.value = JSON.parse(event.value);
                    const topic = event.topic;
                    if (topic in resolvers) {
                        resolvers[topic](event);
                    }
                    else {
                        if ('operator' in resolvers){
                            resolvers['operator'](event);
                        }
                        else {
                            debugConsumer(`No resolver for topic: ${topic}`)
                        }
                    }
                }
                else {
                    debugConsumer('Event was null');
                }
            } catch (err) {
                debugConsumer('Ignoring parsing error: ' + err.message);
            }

        });
        consumer.on('offsetOutOfRange', function (offsetError) {
            debugConsumer(`Consumer offset error on: ${offsetError.topic}`);
            const offset = new kafka.Offset(client);
            offset.fetch([{topic: offsetError.topic, partition: 0, time: -1}], function (err, data) {
                const latestOffset = data[offsetError.topic]['0'][0];
                consumer.setOffset(offsetError.topic, 0, latestOffset);
                debugConsumer(`Consumer current offset: ${latestOffset}`);
            });
        });
        consumer.on('error', (err) => {
            console.log(err)
        });
        producer.on('error', (err) => {
            //console.log('producer');
            //console.log(err);
        });
        producer.sendAll = (messages) => {
            debugProducer(`Sending messages to: ${app.locals.transportationTopics}`);
            return app.locals.transportationTopics.map((el) =>
                producer.sendp([{
                    topic: el,
                    messages: messages,
                }])
            )
        };
        producer.sendp = (payloads) => {
            return new Promise((resolve, reject) => {
                producer.send(payloads, (err, data) => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(data);
                    }
                })
            })
        };
        return new Promise((resolve, reject) => {
            producer.on('ready', () => {
                app.locals.transportationTopics = [];
                app.locals.broker = {
                    client,
                    producer,
                    consumer
                };
                attachResolver('maas', maasHandler(app));
                attachResolver('operator', operatorHandler(app));
                debugClient('Connected to kafka...');
                resolve(app.locals.broker);
            });
            client.on('error', (err) => {
                debugClient('Error connecting to kafka...');
                reject(err);
            });


        });
    }).catch((err) => {
        debugClient(err);
    })
};

const attachResolver = (topic, resolver) => {
    resolvers[topic] = resolver;
};

const getOperators = () => {
    return fetch(`http://${process.env.CATALOG_HOST}/operators`)
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP error, status = ' + response.status);
            }
            return response.json();
        })
        .then(response => {
            return response.map(el => {return {topic: el.id}});
        })
}
module.exports = {
    setup
};
