package gitlet;

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.util.HashMap;
import java.util.Set;

import static gitlet.Utils.*;


/** A class representation of Stage. Similar to Branch class,
 *  Stage class is stored under /.gitlet/stage/STAGE.ser. There will only be
 *  one Stage object created for each gitlet, all that's being updated
 *  are its three fields: addMap, removeMap, and latestCommitID.
 *  addMap and removeMap together will handle add, rm, commit, etc
 *  commands. A file is considered "Marked" if its Value is true.
 *  After each commit and those commands that clears the Stage,
 *  the two HashMaps will be cleared and latestCommitID will be updated.
 *  @author Max Yao
 */
public class Stage implements Serializable {

    /** serialVersionUID to help serialization to identify this object. */
    static final long serialVersionUID = -8903299433050054920L;

    /** A file's name, as Key, is mapped to a boolean, as Value,
     *  that tells the gitlet program whether to be staged or not. */
    private HashMap<String, Boolean> addMap;

    /** A file's name, as Key, is mapped to a boolean, as Value,
     *  that tells the gitlet program whether to remove it or not. */
    private HashMap<String, Boolean> removeMap;

    /** When a Add Command is executed, a temp file is saved to
     *  /.gitlet/stage/ . This HashMap keeps track of which version
     *  of the temp file is going to be committed, so as to not always
     *  commit the latest version. */
    private HashMap<String, String> onStage;

    /** Saves what commit is currently active,
     * and which commit is synced with the current directory. */
    private String latestCommitID;

    /** The single Stage object that will be created to keep track all
     *  working directory files for commands. */
    protected Stage() {
        addMap = new HashMap<>();
        removeMap = new HashMap<>();
        onStage = new HashMap<>();
        latestCommitID = "7fedecda468132e9e388e8062758daa7e8ad1ba9";
    }


    /** Updates the latestCommitID with NEWCMTID. */
    void setLatestCommitID(String newCmtID) {
        latestCommitID = newCmtID;
    }

    /** Returns the latestCommitID. */
    String getLatestCommitID() {
        return latestCommitID;
    }

    /** Returns the Set of all keys in addMap. */
    Set<String> getAddMapFiles() {
        return addMap.keySet();
    }

    /** Returns the Set of all keys in removeMap. */
    Set<String> getRemoveMapFiles() {
        return removeMap.keySet();
    }

    /** Returns the Set of all keys in onStage HashMap. */
    Set<String> getOnStageFiles() {
        return onStage.keySet();
    }

    /** Returns the boolean value associated with FILENAME in addMap.
     *  if the fileName isn't stored, it will return false!! */
    boolean getAddMapMark(String fileName) {
        if (addMap.containsKey(fileName)) {
            return addMap.get(fileName);
        }
        return false;
    }

    /** Returns the boolean value associated with FILENAME in removeMap. */
    boolean getRemoveMapMark(String fileName) {
        return removeMap.get(fileName);
    }

    /** Returns the StoredFileName that Should be committed with the
     *  given FILENAME. */
    String getOnStageStoredName(String fileName) {
        return onStage.get(fileName);
    }

    /** Replaces addMap's HashMap's FILENAME with MARK if such
     *  file is already marked. If such file isn't already stored,
     *  then the pair will be put into the addMap HashMap. */
    void updateAddMap(String fileName, boolean mark) {
        if (addMap.containsKey(fileName)) {
            addMap.replace(fileName, mark);
        } else {
            addMap.put(fileName, mark);
        }
    }

    /** Replaces removeMap's HashMap's FILENAME with MARK if such
     *  file is already marked. If such file isn't already stored,
     *  then the pair will be put into the removeMap HashMap. */
    void updateRemoveMap(String fileName, boolean mark) {
        if (removeMap.containsKey(fileName)) {
            removeMap.replace(fileName, mark);
        } else {
            removeMap.put(fileName, mark);
        }
    }

    /** Replaces onStage HashMap's FILENAME with STOREDNAME if such
     *  file is already mapped to a storedName. If such file isn't already
     *  stored, then the pair will be put into the onStage HashMap. */
    void updateOnStage(String fileName, String storedName) {
        if (onStage.containsKey(fileName)) {
            onStage.replace(fileName, storedName);
        } else {
            onStage.put(fileName, storedName);
        }
    }

    /** Checks and updates the staging area to make sure all staged files
     *  have been modified as to be different from the latestCommit. Else,
     *  it will be removed from all three Maps, as if it was never Added in
     *  the first place, and returns true to indicate to restoreStage!!
     *  else returns false. This should be run whenever we are modifying
     *  /.gitlet/files/ and /commits/, or Status Command. */
    boolean updateStagedFiles() {
        Commit latestCommit = Commit.loadCommit(latestCommitID);
        for (String file : getAddMapFiles()) {
            if (getAddMapMark(file)
                    && latestCommit.getCommittedFiles().contains(file)
                    && storedFileName(new File(file)).equals(
                            latestCommit.getStoredCommittedFileName(file))) {
                removeFileFromStageMaps(file);
                return true;
            }
        }
        return false;
    }

    /** Saving a copy of FILE to /.gitlet/stage/ under the name specified by
     *  storedFileName. Namely: [SHA-1 String]--[file title].[file type]. */
    void saveFileToStage(String file) {
        try {
            File destinationFile = new File(String.format("./.gitlet/stage/%s",
                    storedFileName(new File(file))));
            writeContents(destinationFile, readContents(new File(file)));
        } catch (IllegalArgumentException iae) {
            System.out.printf("An IAE occurred %s%n", iae.getMessage());
        }
    }

    /** Returns the Stage object from /.gitlet/STAGE.ser
     *  will return null if not found. */
    static Stage loadStage() {
        Stage result = null;
        File target = new File("./.gitlet/stage/STAGE.ser");
        if (target.exists()) {
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(target));
                result = (Stage) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException excp) {
                System.out.println("Trouble loading stage: "
                        + excp.getMessage());
            }
        }
        return result;
    }

    /** Serializes this Stage then stores in /.gitlet/STAGE.ser. */
    void storeStage() {
        try {
            File target = new File("./.gitlet/stage/STAGE.ser");
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(target));
            out.writeObject(this);
            out.close();
        } catch (IOException ioe) {
            System.out.println("Trouble storing stage: " + ioe.getMessage());
        }
    }

    /** Deletes all files in /.gitlet/stage/ directory, EXCEPT STAGE.ser,
     *  along with all temporary files in there.
     *  Also, clears the three HashMaps. */
    void clearStageMaps() {
        for (File file : new File("./.gitlet/stage/").listFiles()) {
            if (!file.getName().equals("STAGE.ser")) {
                file.delete();
            }
        }
        addMap.clear();
        removeMap.clear();
        onStage.clear();
    }

    /** Same as UnStage a file.
     *  Removes file FILENAME from all three HashMaps as if it was never added.
     *  The point is so that isEmpty() call may return true if this file was
     *  reverted back to what it was originally without gitlet's knowledge. */
    void removeFileFromStageMaps(String fileName) {
        if (!addMap.containsKey(fileName) || !removeMap.containsKey(fileName)
                || !onStage.containsKey(fileName)) {
            throw new IllegalArgumentException(fileName
                    + " is not in all three Maps!");
        }
        addMap.remove(fileName);
        removeMap.remove(fileName);
        onStage.remove(fileName);
    }

    /** Returns true if FILENAME is currently staged. It is assumed that
     *  this file is in all three HashMaps. */
    boolean isStaged(String fileName) {
        return onStage.containsKey(fileName);
    }

    /** Returns True if both addMap and removeMap are empty. */
    boolean isEmpty() {
        return addMap.isEmpty() && removeMap.isEmpty();
    }

    /** Transfers a FILE from /.gitlet/stage/ to /.gitlet/files/ directory.
     * Throws an IllegalArgumentException if such file isn't found. */
    void transferFileToFilesDir(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("The file trying to transfer "
                    + "to commits doesn't exist!");
        }
        file.renameTo(new File("./.gitlet/files/" + file.getName()));
    }


}

