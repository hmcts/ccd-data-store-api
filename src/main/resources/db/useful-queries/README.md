# 🧹 CCD Data Store Database Cleanup Files

Below is a list of scripts that can be used to safely clean and optimise the **ccd-data-store** database.

---

### 1. `safe_delete_query_case-data_older_than_3_months.sql`

This file provides a **semi-automated cleanup** process.

- Copy the entire script into your SQL client (e.g. DBeaver).  
- Execute it **as a single transaction**.  
- It will automatically create various **functions** and **call them sequentially**.  
- By default it deletes data **older than 3 months**. and in **batches of 1000 records **
- To change that amend these two calls **SELECT prepare_cleanup_temp_tables(8);**, **SELECT run_safe_deletes(1000);**

> ⚙️ **Notes:**
> - The script uses transactions for safety.  
> - You can adjust the 3-month threshold and batch deletion numbers by editing the script logic.

---

### 2. `stored-procedure-safe_delete_query_case-data_older_than_3_months.sql`

This file defines a **stored procedure** (`cleanup_case_data(batch_size int DEFAULT 1000,
    older_than_months int DEFAULT 3)`) for automated clean-up.

- Copy the full script into your SQL client (e.g. DBeaver).  
- Execute it as **one transaction** — this will **create and persist** the stored procedure in the database.  
- Once created, you can call it any time for future clean-ups.

> 💻 **Run command:**
> ```sql
> CALL cleanup_case_data(2000, 3);
> ```
> This executes the stored procedure with a **batch size of 2000**, and **data deletion of 3 months** and older 
> Each iteration deletes records in batches of 2000 rows.  
> If any record in a batch fails to delete, the process automatically falls back to **record-by-record deletion** for reliability.

> 🕐 **Use Case:**
> - Ideal for scheduled clean-up jobs (cron, automation pipelines).  
> - Can also be triggered manually during maintenance windows.

---

### ✅ Summary

| Script | Type | Execution | Description |
|--------|------|------------|--------------|
| `safe_delete_query_case-data_older_than_3_months.sql` | Semi-Automated | Single transaction | Creates and runs helper functions |
| `stored-procedure-safe_delete_query_case-data_older_than_3_months.` | Fully Automated | Stored Procedure | Installs SP for reusable automated clean-ups |

---

**Recommendation:**  
Use **Script 2** for production or regular maintenance, as it is idempotent, batched, and safe for repeat execution.

# 🧹 CCD Data Store Database Cleanup Process
1. Case-data cleanup is optional and is not required for re-indexing. If cleanup is intended, `CALL cleanup_case_data(2000, 3);` deletes data older than 3 months in batches of 2000.
2. Choose the recovery scope: for targeted recovery, retain existing indexes and filter the query to affected cases. Index deletion is not required to requeue cases; do not use a blanket deletion of all Elasticsearch indexes.
3. For a full rebuild after index removal, use **Create Elasticsearch Indices** in `ccd-admin-web` to create missing case-type indexes and configure mappings. Use **Create Global Search Indices** separately if rebuilding Global Search. These actions do not queue case data or delete existing indexes; the SQL script queues cases and Logstash sends their documents to its configured Elasticsearch destinations.
4. Run the entire `logstash_re_indexing_query.sql` DO block with **autocommit enabled**, outside an explicit transaction. It traverses cases across all jurisdictions in primary-key order and commits batches of up to 1000 cases into `case_data_logstash_queue` without updating `case_data`. Logstash can drain the queue while it runs. Set `recovery_name` in the script. Rerun with the same name and unchanged filters to resume from the last committed batch; progress persists in `public.logstash_reindex_progress`. Use a new name for fresh recovery, changed filters or recreated indexes. Completed names do no work. Retain checkpoints until recovery is verified; see the [recovery guidance](../../../../../docs/CCD-4262-logstash-queue-processing.md#failure-recovery) for permissions and cleanup.
5. Verify Elasticsearch delivery and check Logstash output failures/DLQ. Script completion confirms queueing only; resolve failures and re-queue the affected cases if needed.
