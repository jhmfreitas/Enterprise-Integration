const express = require('express');

const router = (db) => {
    const usersRouter = require('./users')(db);
    const eRouter = new express.Router();
    eRouter.use('/users', usersRouter);
    return eRouter;
};

module.exports = router;
