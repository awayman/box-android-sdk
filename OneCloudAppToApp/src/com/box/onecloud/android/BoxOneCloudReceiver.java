package com.box.onecloud.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.box.onecloud.android.Crypto.CryptoException;

/**
 * Abstract BroadcastReceiver for handling braodcasts sent by the Box app and other OneCloud partners. Also provides methods for communicating back to Box.
 * 
 */
public abstract class BoxOneCloudReceiver extends BroadcastReceiver {

    /** Intent action for editing a file. */
    public static final String ACTION_BOX_EDIT_FILE = "com.box.android.EDIT_FILE";

    /** Intent action for creating a file. */
    public static final String ACTION_BOX_CREATE_FILE = "com.box.android.CREATE_FILE";

    /** Intent action for viewing a file. */
    public static final String ACTION_BOX_VIEW_FILE = "com.box.android.VIEW_FILE";

    /** Intent action for uploading a new version of a file. */
    public static final String ACTION_BOX_UPLOAD_NEW_VERSION = "com.box.android.UPLOAD_NEW_VERSION";

    /** Intent action for uploading a new file. */
    public static final String ACTION_BOX_UPLOAD_NEW_FILE = "com.box.android.UPLOAD_NEW_FILE";

    /** Intent action for a file successfully saved to Box. */
    public static final String ACTION_BOX_FILE_SAVED = "com.box.android.FILE_SAVED";

    /** Intent action for a file that is in progress of being saved to Box. */
    public static final String ACTION_BOX_FILE_SAVING = "com.box.android.FILE_SAVING";

    /** Intent action for a file that has failed to be saved to Box. */
    public static final String ACTION_BOX_FILE_SAVED_ERROR = "com.box.android.FILE_SAVED_ERROR";

    /** Intent action for requesting app launch. */
    public static final String ACTION_BOX_LAUNCH = "com.box.android.LAUNCH";

    /** Extras key for file id. */
    public static final String EXTRA_FILE_ID = "com.box.android.EXTRA_FILE_ID";
    /** Extras key for folder id. */
    public static final String EXTRA_FOLDER_ID = "com.box.android.EXTRA_FOLDER_ID";
    /** Extras key for file name. */
    public static final String EXTRA_FILE_NAME = "com.box.android.EXTRA_FILE_NAME";
    /** Extras key for folder name. */
    public static final String EXTRA_FOLDER_NAME = "com.box.android.EXTRA_FOLDER_NAME";
    /** Extras key for Box Token. */
    public static final String EXTRA_BOX_TOKEN = "com.box.android.EXTRA_BOX_TOKEN";
    /** Extras key for the package name of this OneCloud partner app. */
    public static final String EXTRA_ONE_CLOUD_APP_PACKAGE_NAME = "com.box.android.EXTRA_ONE_CLOUD_APP_PACKAGE_NAME";
    /** Extras key for bytes transferred. */
    public static final String EXTRA_BYTES_TRANSFERRED = "com.box.android.EXTRA_BYTES_TRANSFERRED";
    /** Extras key for encryption key. */
    public static final String EXTRA_CRYPTO_KEY = "com.box.android.EXTRA_CRYPTO_KEY";
    /** Extras key for encryption salt. */
    public static final String EXTRA_CRYPTO_SALT = "com.box.android.EXTRA_CRYPTO_SALT";
    /** Extras key for whether or not the file is encrypted. */
    public static final String EXTRA_IS_ENCRYPTED = "com.box.android.EXTRA_IS_ENCRYPTED";

    /** Box package name. */
    private static final String BOX_PACKAGE_NAME = "com.box.android";

    /** Map of tokens to intents. */
    private static final ConcurrentHashMap<Long, Intent> INTENTS = new ConcurrentHashMap<Long, Intent>();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        long boxToken = intent.getLongExtra(EXTRA_BOX_TOKEN, -1);
        if (boxToken < 0) {
            return;
        }

        INTENTS.put(boxToken, intent);

        String cryptoKey = intent.getStringExtra(EXTRA_CRYPTO_KEY);
        String cryptoSalt = intent.getStringExtra(EXTRA_CRYPTO_SALT);

        if (intent.getAction().equals(ACTION_BOX_EDIT_FILE)) {
            try {
                InputStream inputStream = Crypto.getInputStream(new FileInputStream(intent.getData().getPath()), cryptoKey, cryptoSalt);
                onEditFileRequested(context, boxToken, inputStream, intent.getStringExtra(EXTRA_FILE_NAME), intent.getType());
            }
            catch (FileNotFoundException e) {
                return;
            }
            catch (CryptoException e) {
                return;
            }
        }
        else if (intent.getAction().equals(ACTION_BOX_CREATE_FILE)) {
            onCreateFileRequested(context, boxToken, intent.getType());
        }
        else if (intent.getAction().equals(ACTION_BOX_VIEW_FILE)) {
            try {
                InputStream inputStream = Crypto.getInputStream(new FileInputStream(intent.getData().getPath()), cryptoKey, cryptoSalt);
                onViewFileRequested(context, boxToken, inputStream, intent.getStringExtra(EXTRA_FILE_NAME), intent.getType());
            }
            catch (FileNotFoundException e) {
                return;
            }
            catch (CryptoException e) {
                return;
            }
        }
        else if (intent.getAction().equals(ACTION_BOX_FILE_SAVED)) {
            onFileSaved(context, boxToken, intent.getStringExtra(EXTRA_FILE_NAME));
        }
        else if (intent.getAction().equals(ACTION_BOX_FILE_SAVING)) {
            long bytesTransferred = intent.getLongExtra(EXTRA_BYTES_TRANSFERRED, 0);
            if (bytesTransferred > 0) {
                onFileSaving(context, boxToken, intent.getStringExtra(EXTRA_FILE_NAME), bytesTransferred);
            }
        }
        else if (intent.getAction().equals(ACTION_BOX_FILE_SAVED_ERROR)) {
            onFileSavedError(context, boxToken, intent.getStringExtra(EXTRA_FILE_NAME));
        }
        else if (intent.getAction().equals(ACTION_BOX_LAUNCH)) {
            onLaunchRequested(context, boxToken);
        }
    }

    /**
     * Box has requested that you modify an existing Box file. You should load UI for the user to modify the file and save it back to Box by calling
     * uploadNewVersion(). You can call uploadNewVersion() multiple times while the user is editing the file if appropriate (e.g. auto-save every 5 minutes).
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param inputStream
     *            Input stream from which the file data can be read.
     * @param fileName
     *            The file name that the file was saved to Box as.
     * @param type
     *            The mime type of the file (e.g. text/plain).
     */
    public abstract void onEditFileRequested(final Context context, final long boxToken, final InputStream inputStream, final String fileName, final String type);

    /**
     * Box has requested that you create a new file that should be uploaded to Box by calling uploadNewFile(). You should load UI for the user to create a new
     * file of the given type.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param type
     *            The mime type of the file (e.g. text/plain).
     */
    public abstract void onCreateFileRequested(final Context context, final long boxToken, final String type);

    /**
     * Box has requested that you open and show the contents of a file to the user. You should load a read-only UI for the user to view the file.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param inputStream
     *            Input stream from which the file data can be read.
     * @param fileName
     *            The file name that the file was saved to Box as.
     * @param type
     *            The mime type of the file (e.g. text/plain).
     */
    public abstract void onViewFileRequested(final Context context, final long boxToken, final InputStream inputStream, final String fileName, final String type);

    /**
     * Box has requested that you launch your app in no particular mode. You should load general UI that is most appropriate as a starting screen.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     */
    public abstract void onLaunchRequested(final Context context, final long boxToken);

    /**
     * Box has successfully saved a file to the cloud based on your request. For example, if you call uploadNewVersion() or uploadNewFile(), then you can expect
     * this callback method to be executed when the file has been successfully saved to cloud.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param fileName
     *            The file name that the file was saved to Box as.
     */
    public abstract void onFileSaved(final Context context, final long boxToken, final String fileName);

    /**
     * Box is in progress saving a file to the cloud based on your request. For example, if you call uploadNewVersion() or uploadNewFile(), then you can expect
     * this callback method to be executed while the file is being saved to cloud.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param fileName
     *            The file name that the file was saved to Box as.
     * @param bytesTransferred
     *            The number of bytes transferred so far.
     */
    public abstract void onFileSaving(final Context context, final long boxToken, final String fileName, final long bytesTransferred);

    /**
     * Box failed to save a file to the cloud based on your request. For example, if you call uploadNewVersion() or uploadNewFile(), then you can expect this
     * callback method to be executed if the saving to cloud failed.
     * 
     * @param context
     *            The Context in which the receiver is running.
     * @param boxToken
     *            A token that must be passed back to Box. You will need this when trying to send data back to Box.
     * @param fileName
     *            The file name of the file that failed to be saved to Box.
     */
    public abstract void onFileSavedError(final Context context, final long boxToken, final String fileName);

    /**
     * Upload a new version of a file to Box. The file will be renamed to newFileName on Box. If you do not want to rename the file, use
     * uploadNewVersion(boxToken, file).
     * 
     * @param context
     *            Your application's context.
     * @param boxToken
     *            A token that must be passed back to Box. You should have received a token when Box asked you to modify the file.
     * @param file
     *            The file to be uploaded to Box.
     * @param newFileName
     *            The file on Box will be renamed to this.
     * @param isEncrypted
     *            Whether or not the file you provided was encrypted through openEncryptedOutputStream().
     */
    public static void uploadNewVersion(final Context context, final long boxToken, final File file, final String newFileName, boolean isEncrypted) {
        if (INTENTS.get(boxToken) == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_BOX_UPLOAD_NEW_VERSION);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            Utils.getFileExtension(INTENTS.get(boxToken).getStringExtra(EXTRA_FILE_NAME), "").toLowerCase());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        intent.setPackage(BOX_PACKAGE_NAME);
        intent.putExtras(INTENTS.get(boxToken).getExtras());
        intent.putExtra(EXTRA_ONE_CLOUD_APP_PACKAGE_NAME, context.getPackageName());
        if (newFileName != null) {
            intent.putExtra(EXTRA_FILE_NAME, newFileName);
        }
        intent.putExtra(EXTRA_IS_ENCRYPTED, isEncrypted);
        context.sendBroadcast(intent);
    }

    /**
     * Upload a new version of a file to Box. The original filename on Box will be retained.
     * 
     * @param context
     *            Your application's context.
     * @param boxToken
     *            A token that must be passed back to Box. You should have received a token when Box asked you to modify the file.
     * @param file
     *            The file to be uploaded to Box.
     * @param isEncrypted
     *            Whether or not the file you provided was encrypted through openEncryptedOutputStream().
     */
    public static void uploadNewVersion(final Context context, final long boxToken, final File file, boolean isEncrypted) {
        uploadNewVersion(context, boxToken, file, null, isEncrypted);
    }

    /**
     * Upload a new file to Box.
     * 
     * @param context
     *            Your application's context.
     * @param boxToken
     *            A token that must be passed back to Box. You should have received a token when Box asked you to modify the file.
     * @param file
     *            The file to be uploaded to Box. Make sure this is a file that the Box app will be able to access.
     * @param fileName
     *            The file name that Box will suggest for the user to upload the file as. Note that it is not guaranteed that this file name will actually be
     *            used as the user will be given a chance to change it. You should wait for the onFileSaved() callback to be called to know the actual file name
     *            that was saved to Box.
     * @param isEncrypted
     *            Whether or not the file you provided was encrypted through openEncryptedOutputStream().
     */
    public static void uploadNewFile(final Context context, final long boxToken, final File file, final String fileName, boolean isEncrypted) {
        if (INTENTS.get(boxToken) == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_BOX_UPLOAD_NEW_FILE);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(fileName, "").toLowerCase());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        intent.setPackage(BOX_PACKAGE_NAME);
        intent.putExtras(INTENTS.get(boxToken).getExtras());
        intent.putExtra(EXTRA_ONE_CLOUD_APP_PACKAGE_NAME, context.getPackageName());
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_IS_ENCRYPTED, isEncrypted);
        context.sendBroadcast(intent);
    }

    /**
     * Launch Box.
     * 
     * @param context
     *            Your application's context.
     * @param boxToken
     *            A token that must be passed back to Box.
     */
    public static void launchBox(final Context context, final long boxToken) {
        if (INTENTS.get(boxToken) == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_BOX_LAUNCH);
        intent.setPackage(BOX_PACKAGE_NAME);
        intent.putExtras(INTENTS.get(boxToken).getExtras());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.sendBroadcast(intent);
    }

    /**
     * Open an output stream that will encrypt data as it is being written out. For example, you can use this to write out to an encrypted file by passing in a
     * FileOutputStream pointing to your desired file location.
     * 
     * @param boxToken
     *            You should have received a token from Box.
     * @param outputStream
     *            An OutputStream pointing to your desired output location. For example, this could be a FileOutputStream or a ByteArrayOutputStream.
     * @return An output stream that will encrypt data as it is being written out, or null if there was an error.
     */
    public static OutputStream openEncryptedOutputStream(final long boxToken, final OutputStream outputStream) {
        Intent intent = INTENTS.get(boxToken);
        if (intent == null) {
            return null;
        }
        try {
            return Crypto.getOutputStream(outputStream, intent.getStringExtra(EXTRA_CRYPTO_KEY), intent.getStringExtra(EXTRA_CRYPTO_SALT));
        }
        catch (CryptoException e) {
            return null;
        }
    }
}
