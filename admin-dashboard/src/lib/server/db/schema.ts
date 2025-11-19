import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core';

export const devices = sqliteTable('devices', {
    id: text('id').primaryKey(),
    model: text('model').notNull(),
    androidVersion: text('android_version').notNull(),
    registeredAt: integer('registered_at', { mode: 'timestamp' }).notNull(),
    lastLogin: integer('last_login', { mode: 'timestamp' })
});

export const users = sqliteTable('users', {
    id: text('id').primaryKey(),
    username: text('username').notNull().unique(),
    passwordHash: text('password_hash').notNull(),
    role: text('role').notNull().default('child'),
    createdAt: integer('created_at', { mode: 'timestamp' }).notNull()
});

