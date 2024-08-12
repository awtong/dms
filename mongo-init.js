db.getSiblingDB('admin').auth(
    'admin', 'password'
);
db.createUser({
  user: 'user',
  pwd: 'password',
  roles: ["readWrite"],
});