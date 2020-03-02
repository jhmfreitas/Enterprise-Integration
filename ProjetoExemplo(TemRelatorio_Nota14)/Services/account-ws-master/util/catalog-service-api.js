const fetch = require('node-fetch');
const validateService = (operatorId, serviceName, serviceType) => {
    return fetch(`http://${process.env.CATALOG_HOST}/operators/${operatorId}/services`)
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP error, status = ' + response.status);
            }
            return response.json();
        })
        .then(response => {
            if (!(response && Array.isArray(response) && response.filter((el) => {
                return el.name === serviceName && el.operator_id === operatorId && el.service_type.includes(serviceType)
            }).length > 0)) {
                throw new Error(`The service ${serviceName} for operator ${operatorId} isn't available`);
            }
        })
};

module.exports = {
    validateService
};
