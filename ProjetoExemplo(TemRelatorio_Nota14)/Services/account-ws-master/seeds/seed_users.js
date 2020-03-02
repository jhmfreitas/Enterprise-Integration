const uuid = require('uuid/v4');
exports.seed = function(knex, Promise) {
  // Deletes ALL existing entries
  return knex('user').del()
    .then(function () {
      // Inserts seed entries
      return knex('user').insert([
        {
            id: uuid(),
            first_name: 'Pedro',
            last_name: 'Cerejo',
            email: 'pedro.cerejo@tecnico.ulisboa.pt',
            payment_method: 'PayPal',
            nif: 123456886,
            address: 'MEMEMEMEMEMEMES'
        },
      ]);
    });
};
