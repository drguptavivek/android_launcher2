import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { devices } from '$lib/server/db/schema';
import { object, string, parse } from 'valibot';
import type { RequestHandler } from './$types';

const RegisterSchema = object({
    id: string(),
    model: string(),
    androidVersion: string()
});

export const POST: RequestHandler = async ({ request }) => {
    try {
        const body = await request.json();
        const { id, model, androidVersion } = parse(RegisterSchema, body);

        await db.insert(devices).values({
            id,
            model,
            androidVersion,
            registeredAt: new Date(),
            lastLogin: new Date()
        }).onConflictDoUpdate({
            target: devices.id,
            set: {
                model,
                androidVersion,
                lastLogin: new Date()
            }
        });

        return json({ status: 'success', message: 'Device registered' });
    } catch (err) {
        console.error(err);
        return json({ status: 'error', message: 'Invalid request' }, { status: 400 });
    }
};
