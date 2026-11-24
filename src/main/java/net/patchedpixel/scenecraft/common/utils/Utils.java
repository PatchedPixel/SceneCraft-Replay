package net.patchedpixel.scenecraft.common.utils;

import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import com.google.common.base.Throwables;
import com.google.common.net.PercentEscaper;
import net.patchedpixel.scenecraft.common.flamex.util.Image;
import net.minecraft.CrashReport;
import net.patchedpixel.scenecraft.common.flamex.popup.CrashReportScreen;
import net.minecraft.Util;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;
import com.mojang.blaze3d.platform.InputConstants;

public class Utils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final float DEFAULT_MS_PER_TICK = (float) 1000 / 20;

    private static InputStream getResourceAsStream(String path) {
        return Utils.class.getResourceAsStream(path);
    }

    public static final Image DEFAULT_THUMBNAIL;

    static {
        Image thumbnail;
        try {
            thumbnail = Image.read(getResourceAsStream("/assets/scenecraft/unknown.png"));
        } catch(Exception e) {
            thumbnail = new Image(1, 1);
            e.printStackTrace();
        }
        DEFAULT_THUMBNAIL = thumbnail;
    }

    /**
     * Neither the root certificate of LetsEncrypt nor the root that cross-signed it is included in the default
     * Java keystore prior to 8u101.
     * Therefore whenever a connection to the replaymod.com site is made, this SSLContext has to be used instead.
     * It has been constructed to include the necessary root certificates.
     * @see #SSL_SOCKET_FACTORY
     */
    public static final SSLContext SSL_CONTEXT;

    /**
     * @see #SSL_CONTEXT
     */
    public static final SSLSocketFactory SSL_SOCKET_FACTORY;

    static {
        // Largely from https://community.letsencrypt.org/t/134/37
        try (InputStream in = getResourceAsStream("/dst_root_ca_x3.pem")){
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("1", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagerFactory.getTrustManagers(), null);
            SSL_CONTEXT = ctx;
            SSL_SOCKET_FACTORY = ctx.getSocketFactory();
        } catch(IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertSecondsToShortString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(String.format("%02d", hours)).append(":");
        builder.append(String.format("%02d", min)).append(":");
        builder.append(String.format("%02d", sec));

        return builder.toString();
    }

    public static Size fitIntoBounds(Size toFit, Size bounds) {
        int width = toFit.width();
        int height = toFit.height();

        float w = (float) width / bounds.width();
        float h = (float) height / bounds.height();

        if(w > h) {
            height = (int) (height / w);
            width = (int) (width / w);
        } else {
            height = (int) (height / h);
            width = (int) (width / h);
        }

        return new Size(width, height);
    }

    private static final PercentEscaper REPLAY_NAME_ENCODER = new PercentEscaper(".-_ ", false);

    public static Path replayNameToPath(Path folder, String replayName) {
        // If we can, prefer directly using the replay name as the file name
        if(isUsable(folder, replayName + ".mcpr")) {
            return folder.resolve(replayName + ".mcpr");
        } else {
            // otherwise, fall back to percent encoding
            return folder.resolve(REPLAY_NAME_ENCODER.escape(replayName) + ".mcpr");
        }
    }

    /**
     * Checks whether a given file name is actually usable with the file system / operating system at the given folder.
     */
    private static boolean isUsable(Path folder, String fileName) {
        if(fileName.contains(folder.getFileSystem().getSeparator())) {
            return false; // file name contains the name separator, definitely not usable
        }

        Path path;
        try {
            path = folder.resolve(fileName);
        } catch(InvalidPathException e) {
            return false; // file name contains invalid characters, definitely not usable
        }
        if(Files.exists(path)) {
            return true; // if it already exits, it's definitely usable
        }

        // Otherwise, there's no sure way to know, so we just gotta try
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            outputStream.flush();
        } catch(IOException e) {
            return false;
        }

        // Looking good, but now we gotta clean up that mess (and Anti-Virus / Cloud Sync are know to lock them)
        int attempts = 0;
        while(true) {
            try {
                Files.delete(path);
                return true;
            } catch(IOException e) {
                if(attempts++ > 100) {
                    LOGGER.warn("Repeatedly failed to clean up temporary test file at " + path + ": ", e);
                    return false; // while we were able to use it, it's taken now and we can't get it back
                }
            }
        }
    }

    public static String fileNameToReplayName(String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        try {
            return URLDecoder.decode(baseName, Charsets.UTF_8.name());
        } catch(IllegalArgumentException e) {
            return baseName;
        } catch(UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public static boolean isCtrlDown() {
        boolean mac = Util.getPlatform() == Util.OS.OSX;
        return InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), mac ? InputConstants.KEY_LWIN : InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), mac ? InputConstants.KEY_RWIN : InputConstants.KEY_RCONTROL);
    }

    /** Logs and saves the crash report and shows it in a {@link CrashReportScreen} on top of the current screen. */
    public static void error(Logger logger, CrashReport crashReport, Runnable onClose) {
        // Convert crash report to string
        String crashReportStr = crashReport.getFriendlyReport();

        // Log via logger
        logger.error(crashReportStr);

        // Try to save the crash report
        if(crashReport.getSaveFile() == null) {
            try {
                File folder = new File(getMinecraft().gameDirectory, "crash-reports");
                File file = new File(folder, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
                logger.debug("Saving crash report to file: {}", file);
                crashReport.saveToFile(file);
            } catch(Throwable t) {
                logger.error("Saving crash report file:", t);
            }
        } else {
            logger.debug("Not saving crash report as file already exists: {}", crashReport.getSaveFile());
        }

        logger.trace("Opening crash report screen");
        getMinecraft().setScreen(new CrashReportScreen(getMinecraft().screen, crashReportStr, () -> {
            logger.trace("Crash report screen closed");
            if(onClose != null) {
                onClose.run();
            }
        }));
    }

    public static <T> T configure(T instance, Consumer<T> configure) {
        configure.accept(instance);
        return instance;
    }

    /**
     * Like {@link Files#createDirectories(Path, FileAttribute[])} but doesn't explode if it's a symlink.
     */
    public static Path ensureDirectoryExists(Path path) throws IOException {
        // Who in their right mind thought the default behavior of throwing when the target is a link to a directory
        // was the preferred behavior?! Everyone has to fall for this at least once to learn it...
        // https://bugs.openjdk.java.net/browse/JDK-8130464
        return Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
    }
}
