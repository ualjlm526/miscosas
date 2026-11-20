package actividad2ejercicio4;

public abstract class Vehículo {

	Sede sede;
	private String codigo;
	private String marca;
	private double precioDia;

	public double getPrecioDia() {
		return this.precioDia;
	}

	public void setPrecioDia(double precioDia) {
		this.precioDia = precioDia;
	}

	public double calcularPrecioAlquiler(int dias) {
		throw new UnsupportedOperationException();
	}

	public void trasladar(Sede sedeDestino) {
		throw new UnsupportedOperationException();
	}
}
