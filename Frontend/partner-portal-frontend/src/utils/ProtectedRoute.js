import { useSelector } from "react-redux";
import { Navigate } from "react-router-dom";
import { canAccessRoute } from "./permissions";
import { isSandboxMode } from "./sandboxMode";

/**
 * Protected Route component that checks RBAC permissions
 * @param {Object} props
 * @param {string} props.path - The route path to check permission for
 * @param {ReactNode} props.children - The component to render if authorized
 * @returns {ReactNode}
 */
const ProtectedRoute = ({ path, children }) => {
  const userRole = useSelector((state) => state.common.userRole);
  const permissions = useSelector((state) => state.common.permissions);

  // In sandbox mode, grant full access to all routes
  if (isSandboxMode()) {
    return children;
  }

  // Check if user has access to this route
  const hasAccess = canAccessRoute(userRole, permissions, path);

  if (!hasAccess) {
    // Redirect to unauthorized page or dashboard
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
};

export default ProtectedRoute;

