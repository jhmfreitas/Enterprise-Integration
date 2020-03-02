const uuid = require("uuid/v4");
const getUsers = (db) => {
    return () => {
      return db
          .select('*')
          .from('user')
    }
};
const getUser = (db) => {
    return (userId) => {
        return db
            .select('*')
            .from('user')
            .where('id', userId)
            .then(rows => {
                return rows[0];
            })
    };
};
const addUser = (db) => {
    return (userData) => {
        const user = {
            id: uuid(),
            first_name: userData.first_name,
            last_name: userData.last_name,
            email: userData.email,
            payment_method: userData.payment_method,
            address: userData.address,
            nif: userData.nif
        };
        return db
            .insert(user)
            .into('user')
            .then(() => user.id)
    }
};

const addTrip = (db) => {
    return (userId) => {
        return db
            .select('trips')
            .from('user')
            .where('id', userId).first()
            .then(user => {
                if (!user) {
                    throw new Error(`No user with id:${userId}`)
                }
                return db('user')
                    .where('id', userId)
                    .update({
                        trips: user.trips + 1
                    }).then(() => user)

            })
            .then((user) => user.trips + 1)

    }
}

const updateBalance = (db) => {
    return (userId, value) => {
        return db.select('balance')
            .from('user')
            .where('id', userId)
            .then(rows => rows[0])
            .then(user =>
                db
                    .table('user')
                    .where('id', userId)
                    .update({balance: user.balance + value, is_blacklisted: (user.balance + value < 0)})
                    .then(rows => {
                        if (rows || rows.length >= 1) {
                            return Promise.resolve((user.balance + value < 0))
                        } else {
                            return Promise.resolve((user.balance + value < 0))
                        }
                    })
            )
    }
};

const userModel = (db) => {
    return {
        getUsers: getUsers(db),
        getUser: getUser(db),
        addUser: addUser(db),
        addTrip: addTrip(db),
        updateBalance: updateBalance(db),
    }
};

module.exports = userModel;
