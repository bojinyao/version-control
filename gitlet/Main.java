package gitlet;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Formatter;
import java.util.Set;
import java.util.HashSet;


import static gitlet.Utils.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Max Yao
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        quickOperationCheck(args);
        switch (args[0]) {
        case "init":
            doInit(args);
            break;
        case "add":
            doAdd(args);
            break;
        case "commit":
            doCommit(args);
            break;
        case "rm":
            doRm(args);
            break;
        case "log":
            doLog(args);
            break;
        case "global-log":
            doGlobalLog(args);
            break;
        case "find":
            doFind(args);
            break;
        case "status":
            doStatus(args);
            break;
        case "checkout":
            doCheckOut(args);
            break;
        case "branch":
            doBranch(args);
            break;
        case "rm-branch":
            doRmBranch(args);
            break;
        case "reset":
            doReset(args);
            break;
        case "merge":
            doMerge(args);
            break;
        case "delete":
            doDeleteGitlet(args);
            break;
        default:
            printErrMsg("No command with that name exists.");
            exit();
        }
    }

    /** Handles the Init command. Pass in ARGS from main method. */
    static void doInit(String... args) {
        File findGitlet = new File("./.gitlet/");
        if (findGitlet.exists()) {
            printErrMsg("A Gitlet version-control"
                    + " system already exists in the current directory.");
            exit();
        }
        findGitlet.mkdir();
        File filesDir = new File("./.gitlet/files/");
        filesDir.mkdir();
        File commitsDir = new File("./.gitlet/commits/");
        commitsDir.mkdir();
        File stageDir = new File("./.gitlet/stage/");
        stageDir.mkdir();
        new Commit().storeCommit();
        new Branch().storeBranch();
        new Stage().storeStage();
    }

    /** Handles the Add command. Pass in ARGS from main method. */
    static void doAdd(String... args) {
        String fileName = args[1];
        File targetFile = new File(fileName);
        if (!targetFile.exists()) {
            printErrMsg("File does not exist.");
            exit();
        }
        Stage stage = Stage.loadStage();
        Branch branch = Branch.loadBranch();
        String currBranch = branch.getCurrBranch();
        Commit currCommit = branch.getBranchHeadCommitObj(currBranch);
        String storedFileName;
        if (currCommit.getCommittedFiles().contains(fileName)) {
            storedFileName = currCommit.getStoredCommittedFileName(fileName);
            if (identicalFiles(targetFile,
                    new File("./.gitlet/files/" + storedFileName))
                    && fileName.equals(storedFileName.substring(
                            UID_LENGTH + 2))) {
                if (stage.isStaged(fileName)) {
                    stage.removeFileFromStageMaps(fileName);
                    stage.storeStage();
                }
                exit();
            }
        }

        stage.updateAddMap(fileName, true);
        stage.updateRemoveMap(fileName, false);
        stage.saveFileToStage(fileName);
        stage.updateOnStage(fileName, storedFileName(new File(fileName)));
        stage.storeStage();
    }

    /** Handles the Commit command. Pass in ARGS from main method. */
    static void doCommit(String... args) {
        if (args[1].length() == 0) {
            printErrMsg("Please enter a commit message.");
            exit();
        }
        String msg = args[1];
        Stage stage = Stage.loadStage();

        if (stage.isEmpty()) {
            printErrMsg("No changes added to the commit.");
            exit();
        }
        Branch branch = Branch.loadBranch();
        String currBranch = branch.getCurrBranch();
        Commit currBranchCommit = branch.getBranchHeadCommitObj(currBranch);
        Commit newCommit = new Commit(currBranchCommit.getCommitID(), msg);
        newCommit.processStage(stage);
        stage.setLatestCommitID(newCommit.getCommitID());
        stage.storeStage();
        newCommit.storeCommit();
        branch.updateBranchHead(currBranch, newCommit.getCommitID());
        branch.storeBranch();
    }

    /** Handles the Rm command. Pass in ARGS from main method. */
    static void doRm(String... args) {
        String fileName = args[1];
        Stage stage = Stage.loadStage();
        Branch branch = Branch.loadBranch();
        String currBranch = branch.getCurrBranch();
        Commit currCommit = branch.getBranchHeadCommitObj(currBranch);
        boolean fileIsStaged = false;
        boolean isStaged = false;
        boolean isTracked = false;
        if (stage.getAddMapMark(fileName)) {
            isStaged = true;
            stage.removeFileFromStageMaps(fileName);
            fileIsStaged = true;
            stage.storeStage();
        }
        String currCommitStoredName = currCommit.getStoredCommittedFileName(
                fileName);
        if (currCommitStoredName != null) {
            if (fileIsStaged) {
                stage = Stage.loadStage();
            }
            isTracked = true;
            stage.updateAddMap(fileName, false);
            stage.updateOnStage(fileName, currCommitStoredName);
            stage.updateRemoveMap(fileName, true);
            restrictedDelete(fileName);
            stage.storeStage();
        }
        if (!isStaged && !isTracked) {
            printErrMsg("No reason to remove the file.");
            exit();
        }
    }

    /** Handles the Log command. Pass in ARGS from main method. */
    static void doLog(String... args) {
        Branch branch = Branch.loadBranch();
        String currBranch = branch.getCurrBranch();
        String currCommitID = branch.getBranchHeadCommitID(currBranch);
        Commit currCommitObj;
        do {
            currCommitObj = Commit.loadCommit(currCommitID);
            System.out.println(currCommitObj.toString());
            currCommitID = currCommitObj.getParentSha();
        } while (currCommitID != null);
    }

    /** Handles the Global-log command. Pass in ARGS from main method. */
    static void doGlobalLog(String... args) {
        File commits = new File("./.gitlet/commits/");
        Commit existing;
        for (File file : commits.listFiles()) {
            existing = Commit.loadCommit(
                    file.getName().substring(0, UID_LENGTH));
            System.out.println(existing.toString());
        }
    }

    /** Handles the Find command. Pass in ARGS from main method. */
    static void doFind(String... args) {
        String msg = args[1];
        boolean found = false;
        File commits = new File("./.gitlet/commits/");
        Commit existing;
        for (File file : commits.listFiles()) {
            String commitID = file.getName().substring(0, UID_LENGTH);
            existing = Commit.loadCommit(commitID);
            if (existing.getMessage().equals(msg)) {
                found = true;
                System.out.println(commitID);
            }
        }
        if (!found) {
            printErrMsg("Found no commit with that message.");
        }
    }

    /** Handles the Status command. Pass in ARGS from main method. */
    static void doStatus(String... args) {
        Formatter status = new Formatter();
        status.format("=== Branches ===%n");
        Branch branch = Branch.loadBranch();
        Set<String> allBranches = branch.getAllBranches();
        List<String> sortedBranches = asSortedList(allBranches);
        for (String br : sortedBranches) {
            if (branch.getCurrBranch().equals(br)) {
                status.format("*");
            }
            status.format("%s%n", br);
        }
        status.format("%n");
        Stage stage = Stage.loadStage();
        status.format("=== Staged Files ===%n");
        Set<String> allStaged = stage.getAddMapFiles();
        List<String> sortedStaged = asSortedList(allStaged);
        for (String st : sortedStaged) {
            if (stage.getAddMapMark(st)) {
                status.format("%s%n", st);
            }
        }
        status.format("%n");
        status.format("=== Removed Files ===%n");
        Set<String> allRemoved = stage.getRemoveMapFiles();
        List<String> sortedRemoved = asSortedList(allRemoved);
        for (String rm : sortedRemoved) {
            if (stage.getRemoveMapMark(rm)) {
                status.format("%s%n", rm);
            }
        }
        status.format("%n");
        status.format("=== Modifications Not Staged For Commit ===%n");
        status.format("%n");

        status.format("=== Untracked Files ===%n");
        status.format("%n");
        System.out.print(status.toString());
    }

    /** Extra Credit part. Returns a String of modified files in
     *  working directory. Pass in BRANCH and STAGE. */
    static String modifiedUnstaged(Branch branch, Stage stage) {
        Formatter modified = new Formatter();
        String currBranch = branch.getCurrBranch();
        Commit currCommit = branch.getBranchHeadCommitObj(currBranch);

        Set<String> currCommitFiles = currCommit.getCommittedFiles();
        Set<String> onStage = stage.getOnStageFiles();

        HashSet<String> allModified = new HashSet<>();
        HashSet<String> allDeleted = new HashSet<>();

        for (String stagedFile : onStage) {
            File workingVersion = new File(stagedFile);
            if (stage.getAddMapMark(stagedFile)) {
                if (workingVersion.exists()) {
                    File stagedVersion = new File("./.gitlet/stage/"
                            + stage.getOnStageStoredName(stagedFile));
                    if (!identicalFiles(workingVersion, stagedVersion)) {
                        allModified.add(stagedFile);
                    }
                } else {
                    allDeleted.add(stagedFile);
                }
            }
        }
        for (String commitFile : currCommitFiles) {
            File workingVersion = new File(commitFile);
            File commitVersion = new File("./.gitlet/files/"
                    + currCommit.getStoredCommittedFileName(commitFile));
            if (!onStage.contains(commitFile)) {
                if (!workingVersion.exists()) {
                    allDeleted.add(commitFile);
                } else if (!identicalFiles(workingVersion, commitVersion)) {
                    allModified.add(commitFile);
                }
            } else if (!stage.getRemoveMapMark(commitFile)) {
                if (!workingVersion.exists()) {
                    allDeleted.add(commitFile);
                }
            } else {
                continue;
            }
        }
        HashSet<String> all = new HashSet<>(allModified);
        all.addAll(allDeleted);
        List<String> sorted = asSortedList(all);
        for (String file : sorted) {
            if (allModified.contains(file)) {
                modified.format("%s (modified)%n", file);
            } else {
                modified.format("%s (deleted)%n", file);
            }
        }
        return modified.toString();
    }

    /** Extra Credit part. Returns a String of untracked files in
     *  working directory. Pass in BRANCH and STAGE. */
    static String untracked(Branch branch, Stage stage) {
        Formatter untracked = new Formatter();
        String currBranch = branch.getCurrBranch();
        Commit currBranchCommit = branch.getBranchHeadCommitObj(currBranch);
        Set<String> currTrackedFiles = currBranchCommit.getCommittedFiles();
        List<String> plainFiles = plainFilenamesIn(new File("."));
        Set<String> stagedFiles = stage.getOnStageFiles();
        for (String fileName : plainFiles) {
            if ((!currTrackedFiles.contains(fileName)
                    && !stagedFiles.contains(fileName))
                    || stagedFiles.contains(fileName)
                    && stage.getRemoveMapMark(fileName)) {
                untracked.format("%s%n", fileName);
            }
        }
        return untracked.toString();
    }

    /** Handles the Checkout command. Pass in ARGS from main method. */
    static void doCheckOut(String... args) {
        switch (args.length) {
        case 2:
            doCheckOutBranchNameCase3(args);
            break;
        case 3:
            doCheckOutFileNameCase1(args);
            break;
        case 4:
            doCheckOutCommitFileNameCase2(args);
            break;
        default:
            throw new IllegalArgumentException("Un-cought CheckOut"
                    + " Illegal Command!");
        }

    }

    /** Handles the first case of checkout Command.
     *  Pass in ARGS from main method.
     *  java gitlet.Main checkout -- [file name]. */
    static void doCheckOutFileNameCase1(String... args) {
        String fileName = args[2];
        Branch branch = Branch.loadBranch();
        String currBranchName = branch.getCurrBranch();
        Commit headCommit = branch.getBranchHeadCommitObj(currBranchName);
        if (!headCommit.getCommittedFiles().contains(fileName)) {
            printErrMsg("File does not exist in that commit.");
            exit();
        }
        headCommit.restoreFileFromFiles(fileName);
    }

    /** Handles the second case of checkout Command.
     *  Pass in ARGS from main method.
     *  java gitlet.Main checkout [commit id] -- [file name]. */
    static void doCheckOutCommitFileNameCase2(String... args) {
        boolean foundCommit = false;
        String commitID = args[1];
        String fileName = args[3];
        if (commitID.length() < UID_LENGTH) {
            for (File f : new File("./.gitlet/commits/").listFiles()) {
                if (stringsMatch(f.getName(), commitID)) {
                    commitID = f.getName().substring(0, UID_LENGTH);
                    foundCommit = true;
                }
            }
            if (!foundCommit) {
                printErrMsg("No commit with that id exists.");
                exit();
            }
        }
        Commit commit = Commit.loadCommit(commitID);
        if (commit == null) {
            printErrMsg("No commit with that id exists.");
            exit();
        }
        if (!commit.getCommittedFiles().contains(fileName)) {
            printErrMsg("File does not exist in that commit.");
            exit();
        }
        commit.restoreFileFromFiles(fileName);
    }

    /** Handles the third case of checkout Command.
     *  Pass in ARGS from main method.
     *  java gitlet.Main checkout [branch name]. */
    static void doCheckOutBranchNameCase3(String... args) {
        String branchName = args[1];
        Branch branch = Branch.loadBranch();
        String currBranchName = branch.getCurrBranch();
        if (!branch.getAllBranches().contains(branchName)) {
            printErrMsg("No such branch exists.");
            exit();
        }
        if (currBranchName.equals(branchName)) {
            printErrMsg("No need to checkout the current branch.");
            exit();
        }
        Commit currCommit = branch.getBranchHeadCommitObj(currBranchName);
        Set<String> currCommittedFiles = currCommit.getCommittedFiles();

        Commit branchHeadCommit = branch.getBranchHeadCommitObj(branchName);
        Set<String> branchCommittedFiles =
                branchHeadCommit.getCommittedFiles();
        checkUntrackedFilePresence(branchCommittedFiles, currCommittedFiles);
        for (String branchFile : branchCommittedFiles) {
            branchHeadCommit.restoreFileFromFiles(branchFile);
        }
        branch.setCurrBranchTo(branchName);
        for (String currBranchFile : currCommittedFiles) {
            if (!branchCommittedFiles.contains(currBranchFile)) {
                restrictedDelete(currBranchFile);
            }
        }
        branch.storeBranch();

        Stage stage = Stage.loadStage();
        stage.clearStageMaps();
        stage.storeStage();
    }

    /** Handles the Branch command. Pass in ARGS from main method. */
    static void doBranch(String... args) {
        String branchName = args[1];
        Branch branch = Branch.loadBranch();
        if (branch.getAllBranches().contains(branchName)) {
            printErrMsg("branch with that name already exists.");
            exit();
        }
        String currBranchHeadID =
                branch.getBranchHeadCommitID(branch.getCurrBranch());
        branch.updateBranchHead(branchName, currBranchHeadID);
        branch.storeBranch();
    }

    /** Handles the Rm-branch command. Pass in ARGS from main method. */
    static void doRmBranch(String... args) {
        Branch branch = Branch.loadBranch();
        String branchName = args[1];
        if (branchName.equals(branch.getCurrBranch())) {
            printErrMsg("Cannot remove the current branch.");
            exit();
        }
        if (!branch.getAllBranches().contains(branchName)) {
            printErrMsg("A branch with that name does not exist.");
            exit();
        }
        branch.removeBranch(branchName);
        branch.storeBranch();
    }

    /** Handles the Reset command. Pass in ARGS from main method. */
    static void doReset(String... args) {
        boolean foundCommit = false;
        String commitID = args[1];
        if (commitID.length() < UID_LENGTH) {
            for (File f : new File("./.gitlet/commits/").listFiles()) {
                if (stringsMatch(f.getName(), commitID)) {
                    commitID = f.getName().substring(0, UID_LENGTH);
                    foundCommit = true;
                }
            }
            if (!foundCommit) {
                printErrMsg("No commit with that id exists.");
                exit();
            }
        }
        Branch branch = Branch.loadBranch();
        Commit targetCommit = Commit.loadCommit(commitID);
        if (targetCommit == null) {
            printErrMsg("No commit with that id exists.");
            exit();
        }
        Set<String> targetCommitFiles = targetCommit.getCommittedFiles();

        Commit headCommit = branch.getBranchHeadCommitObj(
                branch.getCurrBranch());
        Set<String> headCommitFiles = headCommit.getCommittedFiles();
        checkUntrackedFilePresence(targetCommitFiles, headCommitFiles);
        for (String targetCommitFile : targetCommitFiles) {
            targetCommit.restoreFileFromFiles(targetCommitFile);
        }

        for (String currCommitFile : headCommitFiles) {
            if (!targetCommitFiles.contains(currCommitFile)) {
                restrictedDelete(currCommitFile);
            }
        }

        branch.updateBranchHead(branch.getCurrBranch(), commitID);
        branch.storeBranch();

        Stage stage = Stage.loadStage();
        stage.clearStageMaps();
        stage.storeStage();
    }

    /** Handles the Merge command. Pass in ARGS from main method. */
    static void doMerge(String... args) {
        String givenBranch = args[1];
        Stage stage = Stage.loadStage();
        if (!stage.isEmpty()) {
            printErrMsg("You have uncommitted changes.");
            exit();
        }
        Branch branch = Branch.loadBranch();
        if (!branch.getAllBranches().contains(givenBranch)) {
            printErrMsg("A branch with that name does not exist.");
            exit();
        }
        String currBranch = branch.getCurrBranch();
        if (currBranch.equals(givenBranch)) {
            printErrMsg("Cannot merge a branch with itself.");
            exit();
        }
        Commit currHeadCommit, givenHeadCommit, splitPointCommit;
        try {
            currHeadCommit = branch.getBranchHeadCommitObj(currBranch);
            givenHeadCommit = branch.getBranchHeadCommitObj(givenBranch);
            splitPointCommit = currHeadCommit.splitPointCommitObj(
                    givenHeadCommit);
            if (splitPointCommit.getCommitID().equals(
                    givenHeadCommit.getCommitID())) {
                printErrMsg("Given branch is an ancestor of the"
                        + " current branch.");
                exit();
            }
            if (splitPointCommit.getCommitID().equals(
                    currHeadCommit.getCommitID())) {
                branch.updateBranchHead(currBranch,
                        givenHeadCommit.getCommitID());
                branch.storeBranch();
                printErrMsg("Current branch fast-forwarded.");
                exit();
            }
            mergeConditions(currHeadCommit, givenHeadCommit, splitPointCommit);

            makeMergeCommit(currHeadCommit, givenHeadCommit, branch,
                    currBranch, givenBranch);
        } catch (IOException ioe) {
            printErrMsg("Error occurred in doMerge: " + ioe.getMessage());
            exit();
        }
    }

    /** Continues the Merge Checks. Pass in CURRHEADCOMMIT, GIVENHEADCOMMIT,
     *  SPLITPOINTCOMMIT. Throws FileNotFoundException. */
    static void mergeConditions(Commit currHeadCommit, Commit givenHeadCommit,
                                Commit splitPointCommit)
            throws FileNotFoundException {
        Set<String> currCommitFiles = currHeadCommit.getCommittedFiles();
        Set<String> givenCommitFiles = givenHeadCommit.getCommittedFiles();
        Set<String> splitCommitFiles = splitPointCommit.getCommittedFiles();
        String givenCommitID = givenHeadCommit.getCommitID();
        boolean encounterConflict = false;
        checkUntrackedFilePresence(givenCommitFiles, currCommitFiles);
        for (String file : givenCommitFiles) {
            String givenVersionOfFile =
                    givenHeadCommit.getStoredCommittedFileName(file);
            String currVersionOfFile =
                    currHeadCommit.getStoredCommittedFileName(file);
            String splitVersionOfFile =
                    splitPointCommit.getStoredCommittedFileName(file);
            if (splitVersionOfFile == null && currVersionOfFile == null) {
                doCheckOutCommitFileNameCase2("checkout", givenCommitID, "--",
                        file);
                doAdd("add", file);
            } else if (splitVersionOfFile == null && currVersionOfFile != null
                    && !currVersionOfFile.equals(givenVersionOfFile)) {
                encounterConflict = true;
                writeConflictedFile(file, currHeadCommit, givenHeadCommit);
                doAdd("add", file);
            } else {
                continue;
            }
        }
        mergeContinued(splitCommitFiles, currHeadCommit, givenHeadCommit,
                splitPointCommit, givenCommitID, encounterConflict);
    }

    /** Continue Merge Checks, pass in SPLITCOMMITFILES, CURRHEADCOMMIT,
     *  GIVENHEADCOMMIT, SPLITPOINTCOMMIT, GIVENCOMMITID, ENCOUNTERCONFLICT.
     *  Thows FileNotFoundException. */
    static void mergeContinued(Set<String> splitCommitFiles,
                               Commit currHeadCommit,
                               Commit givenHeadCommit, Commit splitPointCommit,
                               String givenCommitID, boolean encounterConflict)
            throws FileNotFoundException {
        for (String file : splitCommitFiles) {
            String givenVersionOfFile =
                    givenHeadCommit.getStoredCommittedFileName(file);
            String currVersionOfFile =
                    currHeadCommit.getStoredCommittedFileName(file);
            String splitVersionOfFile =
                    splitPointCommit.getStoredCommittedFileName(file);
            if (currVersionOfFile != null && givenVersionOfFile != null) {
                if (!givenVersionOfFile.equals(splitVersionOfFile)
                        && currVersionOfFile.equals(splitVersionOfFile)) {
                    doCheckOutCommitFileNameCase2("checkout", givenCommitID,
                            "--", file);
                    doAdd("add", file);
                } else if (!currVersionOfFile.equals(splitVersionOfFile)
                        && givenVersionOfFile.equals(splitVersionOfFile)) {
                    continue;
                } else if (!currVersionOfFile.equals(givenVersionOfFile)) {
                    encounterConflict = true;
                    writeConflictedFile(file, currHeadCommit, givenHeadCommit);
                    doAdd("add", file);
                } else  {
                    continue;
                }
            } else if (currVersionOfFile != null
                    && givenVersionOfFile == null) {
                if (currVersionOfFile.equals(splitVersionOfFile)) {
                    restrictedDelete(file);
                } else {
                    encounterConflict = true;
                    writeConflictedFile(file, currHeadCommit, givenHeadCommit);
                    doAdd("add", file);
                }
            } else if (currVersionOfFile == null
                    && givenVersionOfFile != null) {
                if (givenVersionOfFile.equals(splitVersionOfFile)) {
                    continue;
                } else {
                    encounterConflict = true;
                    writeConflictedFile(file, currHeadCommit, givenHeadCommit);
                    doAdd("add", file);
                }
            } else {
                continue;
            }
        }
        if (encounterConflict) {
            printErrMsg("Encountered a merge conflict.");
        }
    }

    /** Finishes the entire Merging process and creates a new Special Commit
     *  Object to add to the Head of the active BRANCH, which will be saved.
     *  The STAGE will be processed, updated and saved as usual. Must pass in
     *  CURRHEADCOMMIT, GIVENHEADCOMMIT, CURRBRANCH, and GIVENBRANCH. */
    static void makeMergeCommit(Commit currHeadCommit, Commit givenHeadCommit,
                                  Branch branch,
                                  String currBranch, String givenBranch) {
        Stage stage = Stage.loadStage();
        String msg = String.format("Merged %s into %s.", givenBranch,
                currBranch);
        Commit newCommit = new Commit(currHeadCommit.getCommitID(),
                givenHeadCommit.getCommitID(), msg);
        newCommit.processStage(stage);
        if (Commit.sameCommitContents(newCommit, currHeadCommit)) {
            printErrMsg("No changes added to the commit.");
            exit();
        }
        stage.setLatestCommitID(newCommit.getCommitID());
        stage.storeStage();
        newCommit.storeCommit();
        branch.updateBranchHead(currBranch, newCommit.getCommitID());
        branch.storeBranch();
    }

    /** Pass in Set of files tracked by the target Commit: TARGETCOMMITFILES,
     *  and Set of files tracked by current Commit: CURRCOMMITFILES,
     *  if a file untracked by the current commit risk being over-written by
     *  the target Commit, will print error message and exit. */
    static void checkUntrackedFilePresence(Set<String> targetCommitFiles,
                                         Set<String> currCommitFiles) {
        for (String targetCFile : targetCommitFiles) {
            if (new File(targetCFile).exists()
                    && !currCommitFiles.contains(targetCFile)) {
                printErrMsg("There is an untracked file in the way; "
                        + "delete it or add it first.");
                exit();
            }
        }
    }

    /** Write into FILE when CURRCOMMIT and GIVENCOMMIT run into conflict. */
    static void writeConflictedFile(String file, Commit currCommit,
                                    Commit givenCommit)
            throws FileNotFoundException {
        File currCommitVersion = new File("./.gitlet/files/"
                + currCommit.getStoredCommittedFileName(file));
        File givenCommitVersion = new File("./.gitlet/files/"
                + givenCommit.getStoredCommittedFileName(file));
        File workingFile = new File(file);
        if (!currCommitVersion.exists() && !givenCommitVersion.exists()) {
            throw new FileNotFoundException("Can't write conflicted");
        }
        byte[] line = "\n".getBytes(StandardCharsets.UTF_8);
        byte[] currVersionContents, givenVersionContents;
        if (currCommitVersion.exists()) {
            currVersionContents = readContents(currCommitVersion);
        } else {
            currVersionContents = new byte[] {};
        }
        if (givenCommitVersion.exists()) {
            givenVersionContents = readContents(givenCommitVersion);
        } else {
            givenVersionContents = new byte[] {};
        }
        writeContents(workingFile, "<<<<<<< HEAD", line,
                currVersionContents, "=======", line,
                givenVersionContents, ">>>>>>>", line);
    }

    /** Created for testing and other uses. Deletes the entire .gitlet/
     *  directory, but not the directory in which it exists.
     *  the first ARGS is delete, the second one is gitlet. */
    static void doDeleteGitlet(String... args) {
        try {
            if (args[0].equals("delete") && args[1].equals("gitlet")) {
                File findGitlet = new File("./.gitlet/");
                if (findGitlet.exists()) {
                    deleteDir(findGitlet);
                    return;
                }
                return;
            }
            printErrMsg("Did you spell \"gitlet\" correctly?");
        } catch (IOException ioe) {
            printErrMsg("Trouble deleting gitlet: " + ioe.getMessage());
        }

    }

    /** A brief check of number of ARGS that are passed into main method,
     *  along with very basic syntax check. */
    static void quickOperationCheck(String... args) {
        int len = args.length;
        if (len == 0) {
            printErrMsg("Please enter a command.");
            exit();
        }
        if (!args[0].equals("init")) {
            File findGitlet = new File("./.gitlet/");
            if (!findGitlet.exists()) {
                printErrMsg("Not in an initialized Gitlet directory.");
                exit();
            }
        }
        switch (args[0]) {
        case "init": case "log": case "global-log": case "status":
            if (!(len == 1)) {
                printErrMsg("Incorrect operands.");
                exit();
            }
            break;
        case "add": case "rm": case "find": case "branch":
        case "rm-branch": case "reset": case "merge": case "delete":
        case "commit":
            if (!(len == 2)) {
                printErrMsg("Incorrect operands.");
                exit();
            }
            break;
        case "checkout":
            if (len < 2 || len > 4
                    || (len == 3 && !args[1].equals("--"))
                    || (len == 4 && !args[2].equals("--"))) {
                printErrMsg("Incorrect operands.");
                exit();
            }
            break;
        default:
        }
    }

}
