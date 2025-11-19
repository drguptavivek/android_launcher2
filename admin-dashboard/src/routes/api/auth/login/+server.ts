
import { json } from '@sveltejs/kit';
import { eq } from 'drizzle-orm';
import { db } from '$lib/server/db';
import { users, telemetry } from '$lib/server/db/schema';
import type { RequestHandler } from './$types';
import { randomUUID } from 'crypto';

export const POST: RequestHandler = async ({ request }) => {
    try {
        const { username, password, deviceId } = await request.json();

        if (!username || !password) {
            return json({ error: 'Missing credentials' }, { status: 400 });
        }

        // Find user
        const user = await db.query.users.findFirst({
            where: eq(users.username, username)
        });

        if (!user) {
            return json({ error: 'Invalid credentials' }, { status: 401 });
        }

        // Check password
        const passwordHash = Buffer.from(password).toString('base64');

        if (user.passwordHash !== passwordHash) {
            return json({ error: 'Invalid credentials' }, { status: 401 });
        }

        // Record Telemetry
        try {
            await db.insert(telemetry).values({
                id: randomUUID(),
                userId: user.id,
                deviceId: deviceId || 'unknown',
                type: 'LOGIN',
                data: JSON.stringify({ timestamp: new Date().toISOString() }),
                createdAt: new Date()
            });
        } catch (tError) {
            console.error('Failed to record telemetry:', tError);
            // Don't fail login just because telemetry failed
        }

        // Return success
        return json({
            success: true,
            user: {
                id: user.id,
                username: user.username,
                role: user.role
            }
        });

    } catch (e) {
        console.error('Login error:', e);
        return json({ error: 'Internal server error' }, { status: 500 });
    }
};
