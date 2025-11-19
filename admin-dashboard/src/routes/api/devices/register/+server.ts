import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { devices, deviceRegistrationTokens } from '$lib/server/db/schema';
import { object, string, parse } from 'valibot';
import { and, eq, gte } from 'drizzle-orm';

const RegisterSchema = object({
	registrationCode: string(),
	id: string(),
	model: string(),
	androidVersion: string()
});

export async function POST({ request }) {
	try {
		const body = await request.json();
		const { registrationCode, id, model, androidVersion } = parse(RegisterSchema, body);

		const token = await db.query.deviceRegistrationTokens.findFirst({
			where: and(
				eq(deviceRegistrationTokens.code, registrationCode.toUpperCase()),
				gte(deviceRegistrationTokens.expiresAt, new Date())
			)
		});

		if (!token) {
			return json(
				{ status: 'error', message: 'Invalid or expired registration code' },
				{ status: 400 }
			);
		}

		await db
			.insert(devices)
			.values({
				id,
				model,
				androidVersion,
				description: token.description,
				registeredAt: new Date(),
				lastLogin: new Date()
			})
			.onConflictDoUpdate({
				target: devices.id,
				set: {
					model,
					androidVersion,
					description: token.description,
					lastLogin: new Date()
				}
			});

		await db.delete(deviceRegistrationTokens).where(eq(deviceRegistrationTokens.id, token.id));

		return json({ status: 'success', message: 'Device registered successfully' });
	} catch (err) {
		console.error(err);
		return json({ status: 'error', message: 'Invalid request' }, { status: 400 });
	}
}
