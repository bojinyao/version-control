# Gitlet: Version-Control System
class: Data Structures\
project spec link: [Gitlet](https://inst.eecs.berkeley.edu/~cs61b/fa17/materials/proj/proj3/index.html)

### Command Quick Links:
- [init](#init)
- [add](#add)
- [commit](#commit)
- [rm](#rm)
- [log](#log)
- [global-log](#global-log)
- [find](#find)
- [status](#status)
- [checkout](#checkout)
- [branch](#branch)
- [rm-branch](#rm-branch)
- [reset](#reset)
- [merge](#merge)

### NOTE: 
- **This version control system can ONLY track *.txt* files!**
- **It is highly recommended that you place your *.txt* files within `version-control/` directory**
- **Since gitlet only sees *one* directory above itself, if you are taking `gitlet/` outside of `version-control/`, make sure to take `glet` executable along with it (unless you are familiar with how to run the code)**
- For feature details please feel free to checkout project spec. 
- First time running a command will take slightly longer as glet will have to compile everything. All future commands will be fast. 

## Required Software
- MacOS
- Terminal
  - `make` command in xcode tools: type `xcode-select --install` in Terminal if `command -v make` returns nothing
- Java 8+ [Download](https://www.java.com/en/download/mac_download.jsp)

## Download Instruction
You can `clone` or download(zip) project code onto your desktop. Project will be saved in `version-control` directory.
If project directory is on your desktop, it will look like:
```
Desktop/
  :
  └── version-control/
        ├── gitlet/
        └── glet
```
  
## Commands
### init
  - Usage: `./glet init`
  - Description: Creates a new Gitlet version-control system in the current directory.
### add 
  - Usage: `./glet add [txt file name]`
    - e.g `./glet add wug.txt`
  - Description: Adds a copy of the file as it currently exists to the staging area.
### commit 
  - Usage: `./glet commit [message]`
    - e.g. `./glet commit "added wug.txt"`
  - Description: Saves a snapshot of certain files in the current commit and staging area so they can be restored at a later time, creating a new commit. The commit is said to be tracking the saved files.
### rm 
  - Usage: `./glet rm [txt file name]`
    - e.g. `./glet rm wug.txt`
  - Description: Unstage the file if it is currently staged. If the file is tracked in the current commit, mark it to indicate that it is not to be included in the next commit, and remove the file from the working directory if the user has not already done so. If the file is not tracked, it will not be removed from the working directory.
### log 
  - Usage: `./glet log`
  - Description: Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, following the first parent commit links, ignoring any second parents found in merge commits. (In regular Git, this is what you get with `git log --first-parent`).\
  Example output:
  ```
  ===
   commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
   Date: Thu Nov 9 20:00:05 2017 -0800
   A commit message.

   ===
   commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
   Date: Thu Nov 9 17:01:33 2017 -0800
   Another commit message.

   ===
   commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
   Date: Wed Dec 31 16:00:00 1969 -0800
   initial commit
   ```
### global-log 
  - Usage: `./glet global-log`
  - Description: Like log, except displays information about all commits ever made in any order.
### find 
  - Usage: `./glet find [commit message]`
    - e.g. `./glet find "wug.txt"`
  - Description: Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines. This command does not exist in real git.
### status 
  - Usage: `./glet status`
  - Description: Displays what branches currently exist, and marks the current branch with a `*`. Also displays what files have been staged or marked for untracking. *Note: `Modifications Not Staged For Commit` and `Untracked Files` are not implemented so they are always empty.
### checkout 
  - Usages:
    - 1 `./glet checkout -- [file name]`
      - e.g. `./glet checkout -- wug.txt`
    - 2 `./glet checkout [commit id] -- [file name]`
      - e.g. `./glet checkout a0da1e -- wug.txt`
    - 3 `./glet checkout [branch name]`
      - e.g. `./glet checkout otherbranch`
  - Description:
    - 1 Takes the version of the file as it exists in the head commit, the front of the current branch, and puts it in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged.
    - 2 Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged. **Note: one can provide a unique [commit id] instead of the 40 character id printed in logs.*
    - 3 Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch are deleted. The staging area is cleared, unless the checked-out branch is the current branch.
###branch 
  - Usage: `./glet branch [branch name]`
    - e.g. `./glet branch bandersnatch`
  - Description: Creates a new branch with the given name, and points it at the current head node. This command does NOT immediately switch to the newly created branch (just as in real Git).
###rm-branch 
  - Usage: `./glet rm-branch [branch name]`
    - e.g. `./glet rm-branch bandersnatch`
  - Deletes the branch with the given name. This only means to delete the pointer associated with the branch.
### reset 
  - Usage: `./glet reset [commit id]`
    - e.g. `./glet reset a0da1e`
  - Description: Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch's head to that commit node. **Note: abbreviated commit id can also be used here.*
### merge 
  - Usage: `./glet merge [branch name]`
    - e.g. `./glet merge otherbranch`
  - Description: Merges files from the given branch into the current branch. **Note: Any files modified in different ways in the current and given branches are in conflict, and within the file(s) will be marked conflicting contents:*
  ```
    <<<<<<< HEAD
    contents of file in current branch
    =======
    contents of file in given branch
    >>>>>>>
  ```
