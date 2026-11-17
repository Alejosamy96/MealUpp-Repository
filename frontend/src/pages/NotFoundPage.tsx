import { LinkButton } from "../components/Button";
import { EmptyState } from "../components/ui";

export function NotFoundPage() {
  return (
    <EmptyState icon="alert" title="No encontramos esta página">
      <p>Revisa la dirección o vuelve al inicio.</p>
      <LinkButton to="/" variant="secondary" size="sm">
        Ir al inicio
      </LinkButton>
    </EmptyState>
  );
}
