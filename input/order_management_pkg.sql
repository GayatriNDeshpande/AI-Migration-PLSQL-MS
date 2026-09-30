CREATE OR REPLACE PACKAGE order_management_pkg AS
  TYPE order_rec IS RECORD (
    order_id NUMBER,
    customer_id NUMBER,
    order_date DATE,
    total_amount NUMBER
  );

  TYPE order_tab IS TABLE OF order_rec INDEX BY PLS_INTEGER;

  order_not_found EXCEPTION;
  PRAGMA EXCEPTION_INIT(order_not_found, -20001);

  PROCEDURE create_order(
    p_customer_id IN NUMBER,
    p_order_date IN DATE,
    p_total_amount IN NUMBER,
    p_order_id OUT NUMBER
  );

  PROCEDURE update_order_status(
    p_order_id IN NUMBER,
    p_status IN VARCHAR2
  );

  FUNCTION get_order_total(p_order_id IN NUMBER) RETURN NUMBER;

  PROCEDURE list_orders(
    p_customer_id IN NUMBER,
    p_orders OUT order_tab
  );
END order_management_pkg;
/

CREATE OR REPLACE PACKAGE BODY order_management_pkg AS
  CURSOR c_orders(p_customer_id NUMBER) IS
    SELECT order_id, customer_id, order_date, total_amount
    FROM orders
    WHERE customer_id = p_customer_id;

  PROCEDURE log_audit(p_message IN VARCHAR2) IS
    PRAGMA AUTONOMOUS_TRANSACTION;
  BEGIN
    INSERT INTO audit_log(message, created_at)
    VALUES (p_message, SYSTIMESTAMP);
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
  END log_audit;

  PROCEDURE create_order(
    p_customer_id IN NUMBER,
    p_order_date IN DATE,
    p_total_amount IN NUMBER,
    p_order_id OUT NUMBER
  ) IS
  BEGIN
    INSERT INTO orders(order_id, customer_id, order_date, total_amount, status)
    VALUES (orders_seq.NEXTVAL, p_customer_id, p_order_date, p_total_amount, 'NEW')
    RETURNING order_id INTO p_order_id;

    DBMS_OUTPUT.PUT_LINE('Created order ' || p_order_id);
    log_audit('Created order ' || p_order_id);
  END create_order;

  PROCEDURE update_order_status(
    p_order_id IN NUMBER,
    p_status IN VARCHAR2
  ) IS
    v_exists NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_exists
    FROM orders
    WHERE order_id = p_order_id
    FOR UPDATE NOWAIT;

    IF v_exists = 0 THEN
      RAISE order_not_found;
    END IF;

    UPDATE orders
    SET status = p_status
    WHERE order_id = p_order_id;

    log_audit('Updated order ' || p_order_id || ' to ' || p_status);
  END update_order_status;

  FUNCTION get_order_total(p_order_id IN NUMBER) RETURN NUMBER IS
    v_total NUMBER;
  BEGIN
    SELECT NVL(total_amount, 0) INTO v_total
    FROM orders
    WHERE order_id = p_order_id;

    RETURN v_total;
  END get_order_total;

  PROCEDURE list_orders(
    p_customer_id IN NUMBER,
    p_orders OUT order_tab
  ) IS
    v_index PLS_INTEGER := 0;
  BEGIN
    FOR rec IN c_orders(p_customer_id) LOOP
      v_index := v_index + 1;
      p_orders(v_index).order_id := rec.order_id;
      p_orders(v_index).customer_id := rec.customer_id;
      p_orders(v_index).order_date := rec.order_date;
      p_orders(v_index).total_amount := rec.total_amount;
    END LOOP;
  END list_orders;
END order_management_pkg;
/
