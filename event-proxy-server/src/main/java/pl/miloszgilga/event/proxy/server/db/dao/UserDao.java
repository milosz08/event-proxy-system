package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

public interface UserDao extends ContentInitializer {
  String getUserPasswordHash(String username);

  Boolean userExists(String username);

  void createUser(String username, String hashedDefaultPassword);

  void updateUserPassword(String username, String newHashedPassword, boolean customPassword);

  Boolean userHasDefaultPassword(String username);

  void deleteUsers();
}
