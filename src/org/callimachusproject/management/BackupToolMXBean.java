package org.callimachusproject.management;

import java.io.File;
import java.io.IOException;

public interface BackupToolMXBean {

	boolean isBackupInProgress();

	boolean isRestoreInProgress();

	void backup(String label, File dataDir) throws IOException;

	String getBackupLabels();

	void restoreBackup(String label, File dataDir) throws IOException;

}