package pl.miloszgilga.event.proxy.server.db;

import at.favre.lib.crypto.bcrypt.BCrypt;
import pl.miloszgilga.event.proxy.server.Utils;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.util.Objects;

public class InstancePasswordManager implements ContentInitializer {
  private final UserDao userDao;
  private final String username;
  private final int passwordLength;
  private final int hashStrength;

  public InstancePasswordManager(UserDao userDao, String username, int passwordLength,
                                 int hashStrength) {
    this.userDao = userDao;
    this.username = username;
    this.passwordLength = passwordLength;
    this.hashStrength = hashStrength;
  }

  @Override
  public void init() {
    final Boolean userExists = userDao.userExists(username);
    if (userExists == null) {
      return; // possible error
    }
    // if user not exists, create it with default generated password
    if (!userExists) {
      userDao.deleteUsers(); // delete all previous users

      final String password = Utils.generateSecurePassword(passwordLength);
      final String passwordHash = hash(password);

      userDao.createUser(username, passwordHash);
      printLoginDetails(password);
      return;
    }
    // update password only if user has selected default password
    final Boolean hasDefaultPassword = userDao.userHasDefaultPassword(username);
    if (hasDefaultPassword == null) {
      return; // possible error
    }
    // update password, only if user has default password
    if (!hasDefaultPassword) {
      return;
    }
    final String password = Utils.generateSecurePassword(passwordLength);
    userDao.updateUserPassword(username, hash(password), true);
    printLoginDetails(password);
  }

  public String hash(String rawPassword) {
    return BCrypt.withDefaults().hashToString(hashStrength, rawPassword.toCharArray());
  }

  public boolean verify(String username, String incomingPassword) {
    if (!Objects.equals(this.username, username)) {
      return false;
    }
    final String passwordHash = userDao.getUserPasswordHash(username);
    if (passwordHash == null) {
      return false;
    }
    final BCrypt.Result result = BCrypt.verifyer().verify(incomingPassword.toCharArray(),
      passwordHash.toCharArray());
    return result.verified;
  }

  private void printLoginDetails(String rawPassword) {
    System.out.println("\n\tusername: " + username);
    System.out.println("\tpassword: " + rawPassword + "\n");
  }
}
