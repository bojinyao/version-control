package gitlet;

import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.text.SimpleDateFormat;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.Formatter;
import java.util.Calendar;

import static gitlet.Utils.*;

/** A class representation of a single Commit. Whenever the user
 *  executes a commit, a new instance of this object will be created
 *  and this instance will never be destroyed.
 *  @author Max Yao
 */
public class Commit implements Serializable {

    /** serialVersionUID to help serialization to identify this object. */
    static final long serialVersionUID = 8508304067799595614L;

    /** The commit message. */
    private final String message;

    /** The SHA-1 string code of the parent commit. */
    private final String parentSha;

    /** The SHA-a string code of this commit object. Not the file. */
    private final String commitID;

    /** The Date that this object is created. */
    private final String commitTime;

    /** The CommitID of the merged-in Commit. */
    private String givenParentSha = null;


    /** Key: the original file name, ending in .txt.
     *  Value: the new file name from SHA-1 on it's contents, ending in .txt.
     */
    private HashMap<String, String> oNameVSnName;

    /** Constructor for the init command; the initialization of getLet
     *  in a directory. */
    protected Commit() {

        message = "initial commit";
        parentSha = null;

        Date epoch = new Date(0L);
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        commitTime = dateFormat.format(epoch);

        commitID = sha1("Sentinel ", "Thu Jan 1 00:00:00 1970 +0000");
        oNameVSnName = new HashMap<>();
    }

    /** Constructor for commit commands after the initialization of the
     *  sentinel commit. The Commit objects have pointers to the parent
     *  Commit through its SHA-1 String denote as PRTSHA. The message MSG
     *  that will be passed in is also recorded. */
    protected Commit(String prtSha, String msg) {
        this.parentSha = prtSha;
        this.message = msg;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        commitTime = dateFormat.format(calendar.getTime());
        commitID = sha1(parentSha, commitTime);

        oNameVSnName = new HashMap<>(loadCommit(prtSha).oNameVSnName);
    }

    /** A Commit constructor for merged Commit Object Only! Besides the usual
     *  PRTSHA and MSG, a boolean ISMRGD will be passed in as true to
     *  indicate that it is a merged Commit. And CommitID of the Commit
     *  GVNPRTSHA merged into this Commit. And the current branch CURRBRNCH
     *  that will be used in toString. */
    protected Commit(String prtSha, String gvnPrtSha, String msg) {
        this.givenParentSha = gvnPrtSha;

        this.parentSha = prtSha;
        this.message = msg;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        commitTime = dateFormat.format(calendar.getTime());
        commitID = sha1(parentSha, commitTime);

        oNameVSnName = new HashMap<>(loadCommit(prtSha).oNameVSnName);
    }

    /** Returns the commit message initially passed-in to this object. */
    String getMessage() {
        return message;
    }

    /** Returns the parent SHA-1 String initially passed-in to this object. */
    String getParentSha() {
        return parentSha;
    }

    /** Returns the SHA-1 HashCode for this object. */
    String getCommitID() {
        return commitID;
    }

    /** Returns the CommitTime in milliseconds since the Epoch. */
    String getCommitTime() {
        return commitTime;
    }

    /** Returns a Set of all Files(blobs) of this Commit Object. */
    Set<String> getCommittedFiles() {
        return oNameVSnName.keySet();
    }

    /** Returns the Actual stored name for FILE in oNameVSnName.
     *  This is primarily used to determine if a file had been modified since
     *  the last commit. */
    String getStoredCommittedFileName(String file) {
        return oNameVSnName.get(file);
    }

    /** Updates the HashMap oNameVSnName's FILENAME with a STOREDNAME,
     *  if this fileName already exists, it's value will be replaced,
     *  if it doesn't exist, then it will be added to the HashMap. */
    void updateoNameVSnName(String fileName, String storedName) {
        if (oNameVSnName.containsKey(fileName)) {
            oNameVSnName.replace(fileName, storedName);
        } else {
            oNameVSnName.put(fileName, storedName);
        }
    }

    /** Processes the STAGE. This does most of the Commit Command work except
     *  for some minor detail. Mainly, it goes over new files to be committed
     *  and save to /.gitlet/files/ and updates oNameVSnName HashMap, then it
     *  removes those files that are marked to be untracked. Lastly, it deletes
     *  everything in /.gitlet/stage/ and clears the addMap and removeMap. */
    void processStage(Stage stage) {
        for (String file : stage.getAddMapFiles()) {
            if (stage.getAddMapMark(file)) {
                String storedName = stage.getOnStageStoredName(file);
                updateoNameVSnName(file, storedName);
                stage.transferFileToFilesDir(
                        new File("./.gitlet/stage/" + storedName));
            }
        }
        for (String file : stage.getRemoveMapFiles()) {
            if (stage.getRemoveMapMark(file)) {
                oNameVSnName.remove(file);
            }
        }
        stage.clearStageMaps();
    }

    /** Returns the common Commit Object between this Commit Obj
     *  and OTHERCOMMITOBJ. Essentially finding the splitPoint Obj
     *  between the two. Throws FileNotFoundException if no splitPoint
     *  is found, which should never happen in the first place. */
    Commit splitPointCommitObj(Commit otherCommitObj) throws IOException {
        HashSet<String> commitIDsAtAfterMe = new HashSet<>();
        commitIDsAtAfterMe.add(getCommitID());
        Commit parentCommit = this;
        while (parentCommit.getParentSha() != null) {
            parentCommit = loadCommit(parentCommit.getParentSha());
            commitIDsAtAfterMe.add(parentCommit.getCommitID());
        }
        Commit otherCommitParent = otherCommitObj;
        while (!commitIDsAtAfterMe.contains(otherCommitParent.getCommitID())) {
            otherCommitParent = loadCommit(otherCommitParent.getParentSha());
        }
        if (otherCommitParent == null) {
            throw new FileNotFoundException("Cannot find the split point");
        }
        return otherCommitParent;
    }

    /** Returns the common Commit ID between this Commit Obj
     *  and OTHERCOMMITID. Essentially finding the splitPoint Obj ID
     *  between the two. Throws FileNotFoundException if no splitPoint
     *  isn't found, which should never happen in the first place. */
    String splitPointCommitID(String otherCommitID) throws IOException {
        return splitPointCommitObj(loadCommit(otherCommitID)).getCommitID();
    }

    /** Saving a copy of FILE to /.gitlet/files/ under the name specified by
     *  storedFileName. Namely: [SHA-1 String]--[file title].[file type]. */
    void saveFileToFiles(String file) {
        try {
            File destinationFile = new File(String.format("./.gitlet/files/%s",
                    storedFileName(new File(file))));
            writeContents(destinationFile, readContents(new File(file)));
        } catch (IllegalArgumentException iae) {
            System.out.printf("An IAE occurred %s%n", iae.getMessage());
        }
    }

    /** Restores the FILE from /.gitlet/files/ under the name specified by
     *  storedFileName. */
    void restoreFileFromFiles(String file) {
        try {
            File destinationFile = new File(file);
            String origin = String.format("./.gitlet/files/%s",
                    getStoredCommittedFileName(file));
            writeContents(destinationFile, readContents(new File(origin)));
        } catch (IllegalArgumentException iae) {
            System.out.printf("An IAE occurred %s%n", iae.getMessage());
        }
    }

    /** Returns the Commit with the same commitID CMTID from
     *  /.gitlet/commits/commitID.ser will return null if not found. */
    static Commit loadCommit(String cmtID) {
        Commit result = null;
        File target = new File(String.format(
                "./.gitlet/commits/%s.ser", cmtID));
        if (target.exists()) {
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(target));
                result = (Commit) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException excp) {
                System.out.println("Trouble loading commit: "
                        + excp.getMessage());
            }
        }
        return result;
    }

    /** Serializes this Commit then stores in /.gitlet/commits/commitID.ser. */
    void storeCommit() {
        try {
            File target = new File(String.format(
                    "./.gitlet/commits/%s.ser", commitID));
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(target));
            out.writeObject(this);
            out.close();
        } catch (IOException ioe) {
            System.out.println("Trouble storing commit: " + ioe.getMessage());
        }
    }

    @Override
    public String toString() {
        Formatter result = new Formatter();
        result.format("===%n");
        result.format("commit %s%n", getCommitID());
        if (givenParentSha != null) {
            String firstParent = parentSha.substring(0, 7);
            String givenParent = givenParentSha.substring(0, 7);
            result.format("Merge: %s %s%n", firstParent, givenParent);
        }
        result.format("Date: %s%n", getCommitTime());
        result.format("%s%n", getMessage());
        return result.toString();
    }

    /** Checks if Commit C1 and Commit C2 contains same files of
     *  the same contents. Returns true if they do, false otherwise. */
    static boolean sameCommitContents(Commit c1, Commit c2) {
        Set<String> cOneCommittedFiles = c1.getCommittedFiles();
        Set<String> cTwoCommittedFiles = c2.getCommittedFiles();
        if (cOneCommittedFiles.size() != cTwoCommittedFiles.size()) {
            return false;
        }
        for (String file : cOneCommittedFiles) {
            if (!c1.getStoredCommittedFileName(file).equals(
                    c2.getStoredCommittedFileName(file))) {
                return false;
            }
        }
        return true;
    }
}

