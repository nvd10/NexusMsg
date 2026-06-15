#!/usr/bin/env node

/**
 * Admin CLI tool for managing NexusMsg users.
 *
 * Usage:
 *   node admin.js approve --phone +1234567890
 *   node admin.js approve --user-id abc123
 *   node admin.js pending
 *   node admin.js list-all
 *   node admin.js delete --user-id abc123
 */

const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('../nexusmsg.db');

const ADMIN_KEY = process.env.ADMIN_API_KEY || 'admin-secret';

const args = process.argv.slice(2);
const command = args[0];

function getArg(name) {
  const idx = args.indexOf(name);
  return idx !== -1 ? args[idx + 1] : null;
}

function printUser(user) {
  console.log(`  ID: ${user.id}`);
  console.log(`  Name: ${user.name} | Phone: ${user.phone_number} | Username: ${user.username}`);
  console.log(`  Device: ${user.device_id || 'N/A'}`);
  console.log(`  Approved: ${user.approved === 1 ? 'Yes' : 'No'}`);
  console.log(`  Created: ${user.created_at}`);
  console.log('  ---');
}

switch (command) {
  case 'approve': {
    const userId = getArg('--user-id');
    const phone = getArg('--phone');

    if (!userId && !phone) {
      console.error('Usage: admin.js approve --user-id ID | --phone NUMBER');
      process.exit(1);
    }

    const field = userId ? 'id' : 'phone_number';
    const value = userId || phone;

    db.run(`UPDATE users SET approved = 1, approved_at = datetime('now') WHERE ${field} = ?`, [value], function(err) {
      if (err) {
        console.error('Error:', err.message);
        process.exit(1);
      }
      if (this.changes === 0) {
        console.log('User not found.');
      } else {
        console.log('User approved successfully!');
      }
      db.close();
    });
    break;
  }

  case 'pending': {
    db.all('SELECT * FROM users WHERE approved = 0', (err, rows) => {
      if (err) {
        console.error('Error:', err.message);
        process.exit(1);
      }

      if (rows.length === 0) {
        console.log('No pending registrations.');
      } else {
        console.log(`\n${rows.length} pending registration(s):\n`);
        rows.forEach(printUser);
      }
      db.close();
    });
    break;
  }

  case 'list-all': {
    db.all('SELECT * FROM users ORDER BY created_at DESC', (err, rows) => {
      if (err) {
        console.error('Error:', err.message);
        process.exit(1);
      }

      console.log(`\n${rows.length} total user(s):\n`);
      rows.forEach(printUser);
      db.close();
    });
    break;
  }

  case 'delete': {
    const userId = getArg('--user-id');
    if (!userId) {
      console.error('Usage: admin.js delete --user-id ID');
      process.exit(1);
    }

    db.run('DELETE FROM users WHERE id = ?', [userId], function(err) {
      if (err) {
        console.error('Error:', err.message);
        process.exit(1);
      }
      console.log(`User ${userId} deleted.`);
      db.close();
    });
    break;
  }

  default:
    console.log('NexusMsg Admin CLI');
    console.log('  admin.js pending            — List pending registrations');
    console.log('  admin.js approve --phone X  — Approve by phone number');
    console.log('  admin.js approve --user-id X — Approve by user ID');
    console.log('  admin.js list-all           — List all users');
    console.log('  admin.js delete --user-id X — Delete a user');
    db.close();
    break;
}
