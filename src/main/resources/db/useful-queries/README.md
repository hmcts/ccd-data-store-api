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
1. Execute `CALL cleanup_case_data(2000, 3);` (batches of 2000, data older than 3 months)
2. Delete all ES indexes
3. Login into cwd-admin-web and trigger ES re-indexing (this will create the static indexes)
4. Copy and run the script `logstash_re_indexing_query.sql` (this will mark all cases as logstash_enabled=false, which in turn will trigger the indexing process)