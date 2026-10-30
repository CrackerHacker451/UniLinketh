// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title UniLinkENSv2Subregistry
 * @notice ENSv2 Permissioned Registry + Enhanced Access Control for Newton's Lobby / UniLink OS
 * @dev Implements hierarchical subnames under `unilink.eth` (`newtonslobby.eth`) with:
 *      - Role-based Enhanced Access Control (University KYC Registrar vs. Student Builder vs. Squad Lead)
 *      - Expiring Squad & Builder subnames tied to hackathon sprint deadlines
 *      - ENSIP-25 / ENSIP-26 AI Agent identity text records (`guider.<squad>.unilink.eth`)
 */

interface IEnhancedAccessControl {
    function hasRoles(bytes32 resource, uint256 rolesBitmap, address account) external view returns (bool);
    function grantRoles(bytes32 resource, uint256 rolesBitmap, address account) external;
}

interface IPermissionedResolver {
    function setText(bytes32 node, string calldata key, string calldata value) external;
    function setAddr(bytes32 node, address addr) external;
    function setRecordAlias(bytes32 sourceNode, bytes32 targetNode) external;
}

contract UniLinkENSv2Subregistry {
    // Enhanced Access Control Role Bitmaps (ENSv2 Standard)
    uint256 public constant ROLE_REGISTRAR_ADMIN    = 1 << 0; // University / Platform Admin
    uint256 public constant ROLE_KYC_VERIFIER       = 1 << 1; // Can write verified.college & verified.worldid
    uint256 public constant ROLE_STUDENT_OWNER      = 1 << 2; // Can edit skills, github, bio, swarm.avatar
    uint256 public constant ROLE_AI_AGENT_OPERATOR  = 1 << 3; // Can update ENSIP-26 agent.endpoint & x402 quote

    struct SubnameRecord {
        address owner;
        address resolver;
        uint64 expiresAt;
        uint256 rolesBitmap;
        bool revocableByParent;
        string worldIdNullifierHash;
    }

    bytes32 public immutable parentNode; // namehash("unilink.eth")
    address public immutable defaultPermissionedResolver;
    mapping(bytes32 => SubnameRecord) public subnames;
    mapping(bytes32 => mapping(address => uint256)) public resourceRoles;
    mapping(bytes32 => mapping(string => string)) public textRecords;

    event SubnameMinted(
        bytes32 indexed labelHash,
        string label,
        address indexed owner,
        uint64 expiresAt,
        string worldIdNullifier
    );
    event AgentIdentityRegistered(
        bytes32 indexed agentNode,
        string ensName,
        string ensip26Endpoint,
        string hederaX402Account
    );
    event RoleGranted(bytes32 indexed node, address indexed account, uint256 rolesBitmap);

    constructor(bytes32 _parentNode, address _resolver) {
        parentNode = _parentNode;
        defaultPermissionedResolver = _resolver;
        resourceRoles[_parentNode][msg.sender] = ROLE_REGISTRAR_ADMIN | ROLE_KYC_VERIFIER;
    }

    modifier onlyRole(bytes32 node, uint256 requiredRole) {
        require(
            (resourceRoles[node][msg.sender] & requiredRole) != 0 ||
            (resourceRoles[parentNode][msg.sender] & ROLE_REGISTRAR_ADMIN) != 0,
            "ENSv2 EAC: Missing required role bitmap"
        );
        _;
    }

    /**
     * @notice Mints a verified student builder or squad subname under `unilink.eth`
     */
    function mintBuilderOrSquadSubname(
        string calldata label,
        address studentOrSquadOwner,
        uint64 expiresAt,
        string calldata worldIdNullifier,
        string calldata collegeName,
        string calldata skillsCsv
    ) external onlyRole(parentNode, ROLE_KYC_VERIFIER) returns (bytes32 subNode) {
        bytes32 labelHash = keccak256(bytes(label));
        subNode = keccak256(abi.encodePacked(parentNode, labelHash));
        require(subnames[subNode].expiresAt < block.timestamp, "ENSv2: Subname active");

        subnames[subNode] = SubnameRecord({
            owner: studentOrSquadOwner,
            resolver: defaultPermissionedResolver,
            expiresAt: expiresAt,
            rolesBitmap: ROLE_STUDENT_OWNER,
            revocableByParent: true,
            worldIdNullifierHash: worldIdNullifier
        });

        // Delegate ROLE_STUDENT_OWNER to the student's wallet while retaining KYC role for registrar
        resourceRoles[subNode][studentOrSquadOwner] = ROLE_STUDENT_OWNER;
        resourceRoles[subNode][msg.sender] = ROLE_KYC_VERIFIER;

        // Populate initial verified KYC & profile records
        textRecords[subNode]["verified.college"] = collegeName;
        textRecords[subNode]["verified.worldid"] = worldIdNullifier;
        textRecords[subNode]["com.github"] = label;
        textRecords[subNode]["unilink.skills"] = skillsCsv;

        emit SubnameMinted(labelHash, label, studentOrSquadOwner, expiresAt, worldIdNullifier);
        emit RoleGranted(subNode, studentOrSquadOwner, ROLE_STUDENT_OWNER);
    }

    /**
     * @notice Registers an AI Agent Subname (e.g., `guider.triad-alpha.unilink.eth`) with ENSIP-25/26 records
     */
    function registerSquadAgentSubname(
        bytes32 squadNode,
        string calldata agentLabel,
        address agentOperator,
        string calldata ensip26Endpoint,
        string calldata hederaX402PayTo,
        uint64 expiresAt
    ) external onlyRole(squadNode, ROLE_STUDENT_OWNER) returns (bytes32 agentNode) {
        agentNode = keccak256(abi.encodePacked(squadNode, keccak256(bytes(agentLabel))));
        subnames[agentNode] = SubnameRecord({
            owner: agentOperator,
            resolver: defaultPermissionedResolver,
            expiresAt: expiresAt,
            rolesBitmap: ROLE_AI_AGENT_OPERATOR,
            revocableByParent: true,
            worldIdNullifierHash: subnames[squadNode].worldIdNullifierHash
        });

        resourceRoles[agentNode][agentOperator] = ROLE_AI_AGENT_OPERATOR;
        textRecords[agentNode]["ensip25.agent.verified"] = "true";
        textRecords[agentNode]["ensip26.agent.endpoint"] = ensip26Endpoint;
        textRecords[agentNode]["ensip26.agent.x402.hedera"] = hederaX402PayTo;
        textRecords[agentNode]["ensip26.agent.model"] = "gemini-2.5-flash + hedera-x402";

        emit AgentIdentityRegistered(agentNode, agentLabel, ensip26Endpoint, hederaX402PayTo);
    }

    /**
     * @notice Student can update their own profile records, but CANNOT forge `verified.college` or `verified.worldid`
     */
    function setStudentProfileRecord(
        bytes32 subNode,
        string calldata key,
        string calldata value
    ) external onlyRole(subNode, ROLE_STUDENT_OWNER) {
        require(subnames[subNode].expiresAt > block.timestamp, "ENSv2: Subname expired");
        bytes32 keyHash = keccak256(bytes(key));
        require(
            keyHash != keccak256("verified.college") && keyHash != keccak256("verified.worldid"),
            "ENSv2 EAC: Only KYC Verifier can write verified.* records"
        );
        textRecords[subNode][key] = value;
    }
}
