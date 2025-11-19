import { db } from '$lib/server/db';
import { telemetry, users } from '$lib/server/db/schema';
import { desc, eq } from 'drizzle-orm';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async () => {
    const events = await db.select({
        id: telemetry.id,
        type: telemetry.type,
        deviceId: telemetry.deviceId,
        data: telemetry.data,
        createdAt: telemetry.createdAt,
        username: users.username
    })
        .from(telemetry)
        .leftJoin(users, eq(telemetry.userId, users.id))
        .orderBy(desc(telemetry.createdAt))
        .limit(50);

    return {
        events
    };
};
