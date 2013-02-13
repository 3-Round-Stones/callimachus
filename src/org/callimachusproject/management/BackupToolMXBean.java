package org.callimachusproject.management;

import java.io.IOException;

public interface BackupToolMXBean {

	boolean isBackupInProgress();

	boolean isRestoreInProgress();

	void checkForErrors() throws IOException;

	void createBackup(String label) throws Exception;

	String getBackupLabels();

	void restoreBackup(String label) throws IOException;

}