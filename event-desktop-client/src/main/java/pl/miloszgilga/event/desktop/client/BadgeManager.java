package pl.miloszgilga.event.desktop.client;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.desktop.client.store.NotificationStore;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

import static pl.miloszgilga.event.desktop.client.EventDesktopClientMain.APP_NAME;

public class BadgeManager implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(BadgeManager.class);
  private static final String ICON_PATTERN = "assets/icon/icon%d.png";
  private static final String AUDIO_CLIP_PATH = "assets/notification.mp3";
  private static final double AUDIO_VOLUME = 0.5;
  private static final int[] ICON_SIZES = {16, 32, 64, 256};
  private static final Color BG_COLOR = new Color(178, 34, 34);
  private static final int MAX_VALUE = 9;
  private static final String TITLE_PATTERN = "%s (new events: %d)";

  private final CompositeDisposable disposables = new CompositeDisposable();
  private final Map<Integer, List<Image>> iconCache = new HashMap<>();
  private final List<Image> baseIcons = new ArrayList<>();
  private final AudioClip audioClip = loadAudioClip();

  BadgeManager(Stage stage, NotificationStore store) {
    // initialize
    loadBaseIcons();
    preloadIconCache();
    // listen reactive streams
    final Observable<Integer> countStream = store.getCountStream();
    disposables.add(
      countStream
        .subscribe(count -> Platform.runLater(() -> updateBadge(stage, count)))
    );
    disposables.add(
      countStream
        .buffer(2, 1)
        .filter(pair -> pair.get(1) > pair.get(0)) // only run if new > previous
        .subscribe(pair -> Platform.runLater(() -> Helper.ifNotNull(audioClip, AudioClip::play)))
    );
  }

  private void loadBaseIcons() {
    for (final int iconSize : ICON_SIZES) {
      loadIconImage(iconSize).ifPresent(baseIcons::add);
    }
    LOG.info("Loaded: {} base icons", baseIcons.size());
  }

  private AudioClip loadAudioClip() {
    AudioClip clip = null;
    try {
      final URL resourceUrl = BadgeManager.class.getResource(AUDIO_CLIP_PATH);
      clip = new AudioClip(Objects.requireNonNull(resourceUrl).toExternalForm());
      clip.setVolume(AUDIO_VOLUME);
    } catch (Exception e) {
      LOG.error("Unable to load notification sound: {}", AUDIO_CLIP_PATH);
    }
    return clip;
  }

  private void preloadIconCache() {
    // 0-9 variants plus 9+
    final int badgeTotalVariants = MAX_VALUE + 2;
    for (int i = 0; i <= badgeTotalVariants; i++) {
      final List<Image> badgeIcons = new ArrayList<>();
      for (final Image baseIcon : baseIcons) {
        badgeIcons.add(createBadgeIcon(baseIcon, i));
      }
      iconCache.put(i, badgeIcons);
    }
    LOG.info("Loaded: {} icons with: {} badge variants (total: {})", baseIcons.size(),
      badgeTotalVariants, baseIcons.size() * badgeTotalVariants);
  }

  private void updateBadge(Stage stage, int count) {
    final int key = (count <= 0) ? 0 : Math.min(count, 10);
    final List<Image> iconsToShow = iconCache.get(key);
    if (iconsToShow != null && !iconsToShow.isEmpty()) {
      final String title = (count > 0) ? TITLE_PATTERN.formatted(APP_NAME, count) : APP_NAME;
      stage.setTitle(title);
      stage.getIcons().setAll(iconsToShow);
    }
  }

  private Optional<Image> loadIconImage(int size) {
    final String path = ICON_PATTERN.formatted(size);
    try (final InputStream stream = BadgeManager.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new IOException("Icon not present in classpath directory");
      }
      return Optional.of(new Image(stream));
    } catch (IOException ex) {
      LOG.error("Unable to load icon {}. Cause: {}", path, ex.getMessage());
    }
    return Optional.empty();
  }

  // runs only at startup application, then saves in cache
  private Image createBadgeIcon(Image baseIcon, int notificationCount) {
    if (notificationCount <= 0) {
      return baseIcon;
    }
    final int width = (int) baseIcon.getWidth();
    final int height = (int) baseIcon.getHeight();

    final BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    SwingFXUtils.fromFXImage(baseIcon, bImage);

    final Graphics2D g2d = bImage.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    final double badgeSize = height * 0.60;
    final double badgeX = width - badgeSize;

    g2d.setColor(BG_COLOR);
    g2d.fillOval((int) badgeX, 0, (int) badgeSize, (int) badgeSize);

    g2d.setFont(new Font("System", Font.BOLD, (int) (badgeSize * 0.65)));
    g2d.setColor(Color.WHITE);

    final String text = (notificationCount > MAX_VALUE) ? "%d+".formatted(MAX_VALUE)
      : String.valueOf(notificationCount);

    final FontMetrics fm = g2d.getFontMetrics();
    final int textWidth = fm.stringWidth(text);
    final int textHeight = fm.getAscent() - fm.getDescent();

    final int textX = (int) (badgeX + (badgeSize - textWidth) / 2);
    final int textY = (int) ((badgeSize + textHeight) / 2);

    g2d.drawString(text, textX, textY);
    g2d.dispose();

    return SwingFXUtils.toFXImage(bImage, null);
  }

  @Override
  public void close() {
    disposables.clear();
  }
}
