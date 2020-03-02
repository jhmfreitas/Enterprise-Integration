var express = require('express');
const eRouter = new express.Router();
var v1Router = require('./v1');

const router = (db) => {
  eRouter.get('/', function(req, res, next) {
    res.render('index', { title: 'Express' });
  });

  eRouter.get('/version', (req, res, next) => {
    res.status(200).json({version: process.env.CURRENT_API_VERSION});
  });

  eRouter.use('/v1', v1Router(db));
  return eRouter;
};

module.exports = router;
