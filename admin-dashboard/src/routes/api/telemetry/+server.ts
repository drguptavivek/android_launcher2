import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { telemetry } from '$lib/server/db/schema';
import type { RequestHandler } from './$types';
import { randomUUID } from 'crypto';

export const POST: RequestHandler = async ({ request }) => {
    try {
        const { userId, deviceId, events } = await request.json();

        if (!userId || !Array.isArray(events)) {
            return json({ error: 'Invalid payload' }, { status: 400 });
        }

        const records = events.map((event: any) => ({
            id: randomUUID(),
            userId: userId,
            deviceId: deviceId || 'unknown',
            type: event.type,
            data: JSON.stringify(event.data),
            createdAt: new Date(event.timestamp || Date.now())
        }));

        if (records.length > 0) {
            await db.insert(telemetry).values(records);
        }

        return json({ success: true, count: records.length });

    } catch (e) {
        console.error('Telemetry error:', e);
        return json({ error: 'Internal server error' }, { status: 500 });
    }
};
