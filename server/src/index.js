/**
 * NexusMsg Relay Server
 *
 * Architecture:
 * - PURE RELAY — server never sees plaintext messages.
 * - HTTPS API + WebSocket (wss://) for real-time messaging.
 * - E2E encrypted via ECDH + AES-256-GCM.
 * - 7-character user IDs for easy user lookup.
 * - Group chat support.
 * - WebRTC signaling relay for P2P calls (STUN-based).
 * - Admin approval for registration.
 */

const express = require('express');
const http = require('http');
const https = require('https');
const fs = require('fs');
const crypto = require('crypto');
const WebSocket = require('ws');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

require('dotenv').config();

// ─── Configuration ───

const PORT = parseInt(process.env.PORT) || 443;
const HTTP_PORT = parseInt(process.env.HTTP_PORT) || 80;
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-in-production';
const ADMIN_API_KEY = process.env.ADMIN_API_KEY || 'admin-secret';
const NODE_ENV = process.env.NODE_ENV || 'development';

// ─── Database Setup ───

const db = new sqlite3.Database('./nexusmsg.db');