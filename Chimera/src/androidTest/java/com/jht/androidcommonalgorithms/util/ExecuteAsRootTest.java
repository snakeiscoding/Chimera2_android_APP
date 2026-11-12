package com.jht.androidcommonalgorithms.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.runner.Default;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Note I would love for this to be in the artifact, but these commands require the shared user id
 * to work thus they are here.
 */
@RunWith(AndroidJUnit4.class)
public class ExecuteAsRootTest {

    /**
     * Testing chmod as root.
     *
     *
     */
    @Test
    @Chimera
    @Default
    public void chmodTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testFile = new File(context.getFilesDir(), "testFile.txt").getAbsolutePath();

        checkResult(ExecuteAsRoot.executeCommand("touch " + testFile));
        assertTrue(new File(testFile).exists());
        checkResult(RootCommands.chmod("777", testFile));
        checkResult(ExecuteAsRoot.executeCommand("stat -c %a " + testFile), "777\n");
        checkResult(ExecuteAsRoot.executeCommand("rm " + testFile));
    }


    private void checkResult(SystemCommandResult result) {
        Exception ex = result.ex();
        if(ex != null) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    private void checkResult(SystemCommandResult result, String expectedOutput) {
        checkResult(result);
        assertEquals(expectedOutput, result.output());
    }


    @Test
    @Chimera
    @Default
    public void chmodRecursiveTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testFolder = new File(context.getFilesDir(), "/testFolder").getAbsolutePath();
        String testFile = new File(context.getFilesDir(), "/testFolder/testFile.txt").getAbsolutePath();

        checkResult(ExecuteAsRoot.executeCommand("mkdir -p " + testFolder));
        checkResult(ExecuteAsRoot.executeCommand("touch " + testFile));
        assertTrue(new File(testFile).exists());
        checkResult(RootCommands.chmodRecursive("777", testFolder));
        checkResult(ExecuteAsRoot.executeCommand("stat -c %a " + testFile), "777\n");
        checkResult(ExecuteAsRoot.executeCommand("rm -rf " + testFolder));
    }

    @Test
    @Chimera
    @Default
    public void copyTest() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testFolder = new File(context.getFilesDir(), "/testFolder").getAbsolutePath();
        String testFolder2 = new File(context.getFilesDir(), "/testFolder2").getAbsolutePath();
        String testFile = new File(context.getFilesDir(), "/testFolder/testFile.txt").getAbsolutePath();
        String testFile2 = new File(context.getFilesDir(), "/testFolder2/testFile.txt").getAbsolutePath();

        checkResult(ExecuteAsRoot.executeCommand("mkdir -p " + testFolder));
        checkResult(ExecuteAsRoot.executeCommand("mkdir -p " + testFolder2));
        checkResult(ExecuteAsRoot.executeCommand("touch " + testFile));
        checkResult(RootCommands.cp(testFile, testFile2));
        assertTrue(new File(testFile2).exists());
        checkResult(ExecuteAsRoot.executeCommand("rm " + testFile2));
        String group = ExecuteAsRoot.executeCommand("stat -c %G " + testFile).output().replace("\n", "");
        String user = ExecuteAsRoot.executeCommand("stat -c %U " + testFile).output().replace("\n", "");
        checkResult(RootCommands.cp(testFile, testFile2, user, group, "777"));
        assertTrue(new File(testFile2).exists());
        checkResult(ExecuteAsRoot.executeCommand("stat -c %a " + testFile2), "777\n");
        checkResult(ExecuteAsRoot.executeCommand("stat -c %G " + testFile2), group + "\n");
        checkResult(ExecuteAsRoot.executeCommand("stat -c %U " + testFile2), user + "\n");
        checkResult(ExecuteAsRoot.executeCommand("rm -rf " + testFolder));
        checkResult(ExecuteAsRoot.executeCommand("rm -rf " + testFolder2));

    }


    @Test
    @Chimera
    @Default
    public void commandProcessorTest() {
        CmdProcessHelper cmd = new CmdProcessHelper();
        cmd.addCommand("sync\n");
        checkResult(cmd.execute());
    }
}
