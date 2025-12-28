
export interface InviteStaffRequest {
  email: string;
  role: 'doctor' | 'lab';
}

export interface InviteStaffResponse {
  invitationId: string;
  email: string;
  role: 'doctor' | 'lab';
  // link que enviarás por correo
  enrollmentUrl: string;
  // opcional: expiresAt
}