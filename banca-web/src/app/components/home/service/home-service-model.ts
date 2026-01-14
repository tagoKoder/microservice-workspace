export interface WhoAmIResponse {
  personId: string | number;
  email: string;
  role: 'admin' | 'doctor' | 'lab' | 'patient';
  groups?: string[];
}