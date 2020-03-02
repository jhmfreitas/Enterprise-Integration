
exports.up = function(knex, Promise) {
  return knex.schema.createTable('user', (t) => {
      t.uuid('id').primary('id');
      t.string('email').unique().notNullable();
      t.string('first_name').notNullable();
      t.string('last_name').notNullable();
      t.string('address').notNullable();
      t.string('nif').unique().notNullable();
      t.enum('payment_method', ['PayPal', 'CreditCard']);
      t.integer('balance').notNullable().defaultTo(0);
      t.boolean('is_blacklisted').notNullable().defaultTo(false);
  });
};

exports.down = function(knex, Promise) {
    return knex.schema.dropTable('user');
};
