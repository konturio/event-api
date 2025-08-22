# Updating static data

**File processing occurs:**
* when the server is restarted (not suitable for prod);
* by scheduler: it checks the update date of folders and files **daily** to find updated data:
  * if the folder has not been updated, the scheduler does not go further;
  * if the folder has been updated, the scheduler checks the update date of the files inside:
    * if the file has not been updated, the scheduler ignores it;
    * if the file has been updated (or created), the scheduler starts the file import process.

The following cases are possible when processing a file:
* **observation has not changed** -> the system verifies it with the observation in the data_lake, confirms the identity, nothing happens;
* **observation removed from file** -> the mechanism for revocation observations is triggered;
* **new observation has appeared** -> new version of observation falls into data_lake;
* **observation has changed** -> new version of observation falls into data_lake, the mechanism for revocation observations is triggered for old version.

On the example of database tables:

<https://drive.google.com/file/d/1xha5gCeEimh4NaBlm1LX6KzxGpkWKMw3/view?usp=sharing>

Details of technical implementation:
* write from the data lake by the provider all the hashes of the strings in the map as keys (one call to the database by the provider);
* sequentially form the hash of the object from the features of the updated S3 file (with the same provider name);
* compare the S3 hash with the database hash (map object);
* if we find a match, we mark the element with the value "1";
* if we do not find it, we write the object into the map as a new element, send it to the data lake, mark the element with the value "1";
* if at the end of the procedure there are unmarked elements - they need to be "revoked".
