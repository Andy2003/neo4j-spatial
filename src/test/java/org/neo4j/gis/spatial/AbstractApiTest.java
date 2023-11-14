package org.neo4j.gis.spatial;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;


import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public abstract class AbstractApiTest {

    private DatabaseManagementService databases;
    protected GraphDatabaseService db;

    @Before
    public void setUp() throws KernelException, IOException {
        Path dbRoot = new File("target/procedures").toPath();
        FileUtils.deleteDirectory(dbRoot);
        databases = new TestDatabaseManagementServiceBuilder(dbRoot)
            .setConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("spatial.*"))
            .impermanent()
            .build();
        db = databases.database(DEFAULT_DATABASE_NAME);
        registerApiProceduresAndFunctions();
    }

    protected abstract void registerApiProceduresAndFunctions() throws KernelException;

    protected void registerProceduresAndFunctions(Class<?> api) throws KernelException {
        GlobalProcedures procedures = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(GlobalProcedures.class);
        procedures.registerProcedure(api);
        procedures.registerFunction(api);
    }

    @After
    public void tearDown() {
        databases.shutdown();
    }

    protected long execute(String statement) {
        return execute(statement, null);
    }

    protected long execute(String statement, Map<String, Object> params) {
        try (Transaction tx = db.beginTx()) {
            if (params == null) {
                params = Collections.emptyMap();
            }
            long count = Iterators.count(tx.execute(statement, params));
            tx.commit();
            return count;
        }
    }

    protected void executeWrite(String call) {
        try (Transaction tx = db.beginTx()) {
            tx.execute(call).accept(v -> true);
            tx.commit();
        }
    }

    protected Node createNode(String call, String column) {
        return (Node) executeObject(call, null, column);
    }

    protected Object executeObject(String call, String column) {
        return executeObject(call, null, column);
    }

    protected Object executeObject(String call, Map<String, Object> params, String column) {
        Object obj;
        try (Transaction tx = db.beginTx()) {
            if (params == null) {
                params = Collections.emptyMap();
            }
            ResourceIterator<Object> values = tx.execute(call, params).columnAs(column);
            obj = values.next();
            values.close();
            tx.commit();
        }
        return obj;
    }
}
