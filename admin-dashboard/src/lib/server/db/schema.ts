import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core';

export const devices = sqliteTable('devices', {
    id: integer('id').primaryKey({ autoIncrement: true }),
    model: text('model').notNull(),
    androidVersion: text('android_version').notNull(),
    description: text('description'),
    registeredAt: integer('registered_at', { mode: 'timestamp' }).notNull(),
    lastLogin: integer('last_login', { mode: 'timestamp' })
});

export const deviceRegistrationTokens = sqliteTable('device_registration_tokens', {
    id: text('id').primaryKey(),
    code: text('code').notNull().unique(),
    description: text('description'),
    expiresAt: integer('expires_at', { mode: 'timestamp' }).notNull(),
    createdAt: integer('created_at', { mode: 'timestamp' }).notNull()
});

export const users = sqliteTable('users', {
    id: text('id').primaryKey(),
    username: text('username').notNull().unique(),
    passwordHash: text('password_hash').notNull(),
    role: text('role').notNull().default('child'),
    createdAt: integer('created_at', { mode: 'timestamp' }).notNull()
});

export const telemetry = sqliteTable('telemetry', {
    id: text('id').primaryKey(),
    userId: text('user_id').references(() => users.id),
    deviceId: text('device_id'),
    type: text('type').notNull(), // e.g., 'LOGIN', 'APP_OPEN'
    data: text('data'), // JSON string for extra details
    createdAt: integer('created_at', { mode: 'timestamp' }).notNull()
});

