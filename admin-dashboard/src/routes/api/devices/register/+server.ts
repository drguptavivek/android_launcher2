import { json } from '@sveltejs/kit';
import { db } from '$lib/server/db';
import { devices, deviceRegistrationTokens } from '$lib/server/db/schema';
import { object, string, parse } from 'valibot';
import { eq, gte } from 'drizzle-orm';

const RegisterSchema = object({
	registrationCode: string(),
	model: string(),
	androidVersion: string()
});

export async function POST({ request }) {
	try {
		const body = await request.json();
		const { registrationCode, model, androidVersion } = parse(RegisterSchema, body);

		const token = await db.query.deviceRegistrationTokens.findFirst({
			where: (tokens, { and, eq, gte }) => and(
				eq(tokens.code, registrationCode.toUpperCase()),
				gte(tokens.expiresAt, new Date())
			)
		});

		if (!token) {
			return json(
				{ status: 'error', message: 'Invalid or expired registration code' },
				{ status: 400 }
			);
		}

		const registeredAt = new Date();

		const result = await db
			.insert(devices)
			.values({
				model,
				androidVersion,
				description: token.description,
				registeredAt: registeredAt,
				lastLogin: new Date()
			})
			.returning({ insertedId: devices.id });

		const newDeviceId = result[0].insertedId.toString();

		await db.delete(deviceRegistrationTokens).where(eq(deviceRegistrationTokens.id, token.id));

		return json({
			status: 'success',
			message: 'Device registered successfully',
			deviceId: newDeviceId,
			description: token.description,
			registeredAt: registeredAt.toISOString()
		});
	} catch (err) {
		console.error(err);
		return json({ status: 'error', message: 'Invalid request' }, { status: 400 });
	}
}
