BTFU is a minecraft server backup mod designed to be useful and not suck.

To be more specific, instead of a homebrewed half-baked jvm attempt at incremental backups... or worse, not even
supporting incremental backups, BTFU uses standard tools to carry out an age-old backup strategy that works well.
- rsync -ra
- cp -al

In layman's terms, the strategy is to incrementally sync your file tree to a model backup directory, then hardlink-copy
it to a datestamped backup directory.  The result is a series of directories that individually appear to be complete
snapshots of your minecraft server directory, but share underlying data.  Each backup will only take up disk space for
whatever files have changed since the last backup.

BTFU will intelligently cull your backups, to limit space usage.  By "intelligently" I mean, it will delete some, but
not all, older backups, leaving you with a complete chronology of your world, but less frequent snapshots the farther
back you go.  By default it will keep 128 backups.  You choose the number, and BTFU chooses what to delete.

BTFU will back up every 5 minutes, because it can.  And your server will be fine with that, because using the right tool
for the job works wonders.

BTFU assumes control of the save-off and save-on commands.  It turns off saving for each backup, then turns it back on.

BTFU is developed for linux.  Normal people host their minecraft servers on linux, and you should too.  It might work on
other platforms, and I don't care, but I will gladly accept compatibility improvements as long as they don't scare me.

BTFU will not let you run the game if you have not configured a backup directory.  BTFU believes you must have installed
it for a reason, and thinking you're backing up when you're not, sucks.  BTFU will not let you run the game *from* the
backup directory.  Your backups share files via hardlinks, and must be COPIED (not moved), or else they will corrupt
the other backups.

BTFU wants you to stop worrying and relax.
