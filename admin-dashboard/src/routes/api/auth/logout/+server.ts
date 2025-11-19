import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { telemetry } from '$lib/server/db/schema';
import type { RequestHandler } from './$types';
import { randomUUID } from 'crypto';

export const POST: RequestHandler = async ({ request }) => {
    try {
        const { userId, deviceId } = await request.json();

        if (!userId) {
            return json({ error: 'Missing userId' }, { status: 400 });
        }

        // Record Telemetry
        await db.insert(telemetry).values({
            id: randomUUID(),
            userId: userId,
            deviceId: deviceId || 'unknown',
            type: 'LOGOUT',
            data: JSON.stringify({ timestamp: new Date().toISOString() }),
            createdAt: new Date()
        });

        return json({ success: true });

    } catch (e) {
        console.error('Logout error:', e);
        return json({ error: 'Internal server error' }, { status: 500 });
    }
};
