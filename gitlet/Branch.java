package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.HashMap;
import java.util.Set;

/** The Branch object keeps track of ALL HEADs in every existing branch.
 *  There will only be one Branch object for every .gitlet directory,
 *  and this Branch object will be written as /.gitlet/HEAD.ser. This object
 *  keeps track of the HEADs in a HashMap; the keys are the branch names
 *  and the value is the head CommitID of each branch. For example:
 *  "master" --> "askd2j3s..23jjks", "other" --> "sdwhk32...kew".
 *  To keep track of the working branch, we use a String variable called
 *  currBranch.
 *  @author Max Yao
 */
public class Branch implements Serializable {

    /** serialVersionUID to help serialization to identify this object. */
    static final long serialVersionUID = -4038156951416156249L;

    /** A HashMap that keeps track of ALL HEAD Commits in every branch.
     *  Whenever a doCommit, or switching branches, etc. this gets updated.
     *  For example:
     *  "master" --> "askd2j3s..23jjks", "other" --> "sdwhk32...kew".
     */
    private final HashMap<String, String> branchVSHead = new HashMap<>();

    /** A String that keeps track of which branch is being worked on. Defaul
     *  to "master". */
    private String currBranch;

    /** The single Branch object that will be created to keep track of all
     *  HEAds in every branch. */
    protected Branch() {
        branchVSHead.put("master",
                "7fedecda468132e9e388e8062758daa7e8ad1ba9");
        currBranch = "master";
    }


    /** Replaces branchVSHead HashMap's BRANCH head with CMTID if such
     *  branch is already stored. If such branch isn't already stored,
     *  then the pair will be put into the branchVSHead HashMap. */
    void updateBranchHead(String branch, String cmtID) {
        if (branchVSHead.containsKey(branch)) {
            branchVSHead.replace(branch, cmtID);
        } else {
            branchVSHead.put(branch, cmtID);
        }
    }

    /** Removes the BRANCH from branchVSHead HashMap. Throws
     *  IllegalArgumentException if such branch isn't found. */
    void removeBranch(String branch) {
        if (branchVSHead.containsKey(branch)) {
            branchVSHead.remove(branch);
        } else {
            throw new IllegalArgumentException(branch
                    + " can't be found in HashMap");
        }
    }

    /** Returns the Branch object from /.gitlet/commits/commitID.ser
     *  will return null if not found. */
    static Branch loadBranch() {
        Branch result = null;
        File target = new File("./.gitlet/HEAD.ser");
        if (target.exists()) {
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(target));
                result = (Branch) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException excp) {
                System.out.println("Trouble loading branch: "
                        + excp.getMessage());
            }
        }
        return result;
    }

    /** Serializes this Branch then stores in /.gitlet/HEAD.ser. */
    void storeBranch() {
        try {
            File target = new File("./.gitlet/HEAD.ser");
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(target));
            out.writeObject(this);
            out.close();
        } catch (IOException ioe) {
            System.out.println("Trouble storing branch: " + ioe.getMessage());
        }
    }

    /** Returns the current branch. */
    String getCurrBranch() {
        return currBranch;
    }

    /** Returns all existing branches in a Set. */
    Set<String> getAllBranches() {
        return branchVSHead.keySet();
    }

    /** Returns the BRANCH's Head's CommitID. */
    String getBranchHeadCommitID(String branch) {
        return branchVSHead.get(branch);
    }

    /** Returns the BRANCH's Head's Commit Object. */
    Commit getBranchHeadCommitObj(String branch) {
        return Commit.loadCommit(branchVSHead.get(branch));
    }

    /** Update currBranch to BRANCH. Throws IllegalArgumentException if
     *  the new Branch isn't stored in branchVSHead HashMap. */
    void setCurrBranchTo(String branch) {
        if (branchVSHead.containsKey(branch)) {
            currBranch = branch;
        } else {
            throw new IllegalArgumentException(branch
                    + " isn't stored in HashMap");
        }
    }
}
