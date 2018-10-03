/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.drive.sample.demo;

import android.util.Log;

import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * An activity to illustrate how to create a file
 * while performing the contents upload in a separate Thread.
 */
public class CreateFileWithThreadWrappingActivity extends BaseDemoActivity {
    private static final String TAG = "CreateFileActivity";

    @Override
    protected void onDriveClientReady() {
        createFile();
    }

    private void createFile() {
        // [START drive_android_create_file]
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        final Task<DriveContents> fillContentsTask =
                createContentsTask.continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    final TaskCompletionSource<DriveContents> source = new TaskCompletionSource<>();
                    new Thread(() -> {
                        try (OutputStream outputStream = contents.getOutputStream();
                             Writer writer = new OutputStreamWriter(outputStream)){
                            writer.write("Hello World!");
                        } catch (IOException e) {
                            source.setException(e);
                            return;
                        }
                        source.setResult(contents);
                    }).start();
                    return source.getTask();
        });

        Tasks.whenAll(rootFolderTask, fillContentsTask)
                .continueWithTask(task -> {
                    DriveFolder parent = rootFolderTask.getResult();
                    DriveContents contents = fillContentsTask.getResult();

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                          .setTitle("HelloWorld.txt")
                                                          .setMimeType("text/plain")
                                                          .setStarred(true)
                                                          .build();

                    return getDriveResourceClient().createFile(parent, changeSet, contents);
                })
                .addOnSuccessListener(this,
                        driveFile -> {
                            showMessage(getString(R.string.file_created,
                                    driveFile.getDriveId().encodeToString()));
                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                    showMessage(getString(R.string.file_create_error));
                    finish();
                });
        // [END drive_android_create_file]
    }
}
