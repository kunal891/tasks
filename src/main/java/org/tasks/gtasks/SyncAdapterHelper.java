package org.tasks.gtasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.AccountManager;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import timber.log.Timber;

public class SyncAdapterHelper {

    private static final String AUTHORITY = "org.tasks";

    private final AccountManager accountManager;
    private final Preferences preferences;
    private final GtasksPreferenceService gtasksPreferenceService;

    @Inject
    public SyncAdapterHelper(AccountManager accountManager, Preferences preferences,
                             GtasksPreferenceService gtasksPreferenceService) {
        this.accountManager = accountManager;
        this.preferences = preferences;
        this.gtasksPreferenceService = gtasksPreferenceService;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public boolean initiateManualSync() {
        Account account = getAccount();
        if (account == null) {
            return false;
        }
        Bundle extras = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, AUTHORITY, extras);
        return true;
    }

    public boolean isEnabled() {
        return preferences.getBoolean(R.string.sync_gtasks, false) && getAccount() != null;
    }

    public boolean masterSyncEnabled() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    public void enableSynchronization(boolean enabled) {
        Account account = getAccount();
        if (account != null) {
            Timber.d("enableSynchronization=%s", enabled);
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            if (enabled) {
                setSynchronizationInterval(preferences.getIntegerFromString(R.string.gtasks_GPr_interval_key, 0));
            } else {
                setSynchronizationInterval(0);
            }
        }
    }

    public void setSynchronizationInterval(int seconds) {
        Account account = getAccount();
        if (account != null) {
            boolean syncAutomatically = seconds > 0;
            ContentResolver.setSyncAutomatically(account, AUTHORITY, syncAutomatically);
            Timber.d("syncAutomatically=%s, syncInterval=%s", syncAutomatically, seconds);
            if (syncAutomatically) {
                ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, seconds);
            } else {
                ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
            }
        }
    }

    private Account getAccount() {
        return accountManager.getAccount(gtasksPreferenceService.getUserName());
    }

    public boolean shouldShowBackgroundSyncWarning() {
        return isEnabled() && !masterSyncEnabled() && !ContentResolver.getPeriodicSyncs(getAccount(), AUTHORITY).isEmpty();
    }
}